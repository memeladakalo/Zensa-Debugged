package com.zensa.app.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.StudySessionRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ZenTimerViewModel
 *
 * A countdown timer with start / pause / resume / stop behaviour.
 * On completion (or explicit stop with elapsed time), the session
 * is recorded to Room and best-effort pushed to the server.
 *
 * Design notes:
 *   - A single coroutine Job drives the countdown.
 *   - On pause, the Job is cancelled and remaining seconds preserved.
 *   - On resume, a fresh Job starts from remaining seconds.
 *   - On finish, secondsRemaining reaches 0 and a session is recorded.
 *   - On stop, if at least 60 seconds were counted, we record what
 *     was done. Otherwise we discard it (too short to be meaningful).
 */
class ZenTimerViewModel(
    private val studySessionRepository: StudySessionRepository =
        ServiceLocator.studySessionRepository,
    private val moduleRepository: ModuleRepository =
        ServiceLocator.moduleRepository,
    private val tokenStorage: TokenStorage =
        ServiceLocator.tokenStorage(),
    private val userPreferences: UserPreferences =
        ServiceLocator.userPreferences()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZenTimerUiState())
    val uiState: StateFlow<ZenTimerUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        loadDefaults()
        loadModules()
    }

    private fun loadDefaults() {
        viewModelScope.launch {
            val minutes = userPreferences.defaultStudyDurationMinutes.first()
            _uiState.update {
                it.copy(
                    selectedMinutes = minutes,
                    secondsRemaining = minutes * 60,
                    totalSeconds = minutes * 60
                )
            }
        }
    }

    private fun loadModules() {
        viewModelScope.launch {
            val userId = tokenStorage.getUserId()
            val list = moduleRepository.observeModules(userId).first()
            _uiState.update { it.copy(availableModules = list) }
        }
    }

    // ---------------- Duration selection ----------------

    fun onSelectDuration(minutes: Int) {
        if (_uiState.value.isRunning || _uiState.value.isPaused) return
        _uiState.update {
            it.copy(
                selectedMinutes = minutes,
                secondsRemaining = minutes * 60,
                totalSeconds = minutes * 60,
                isFinished = false,
                infoMessage = null
            )
        }
    }

    fun onSelectModule(moduleLocalId: String?) {
        _uiState.update { it.copy(selectedModuleLocalId = moduleLocalId) }
    }

    // ---------------- Timer control ----------------

    fun start() {
        if (_uiState.value.isRunning) return

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isFinished = false,
                startedAtIso = it.startedAtIso ?: now,
                infoMessage = null
            )
        }
        startTicking()
    }

    fun pause() {
        if (!_uiState.value.isRunning) return
        tickJob?.cancel()
        tickJob = null
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resume() {
        if (!_uiState.value.isPaused) return
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        startTicking()
    }

    /**
     * Stops the timer. If at least 60 seconds elapsed, records the
     * partial session. Otherwise resets without recording.
     */
    fun stop() {
        tickJob?.cancel()
        tickJob = null

        val s = _uiState.value
        val elapsed = s.totalSeconds - s.secondsRemaining

        if (elapsed >= MIN_RECORDABLE_SECONDS && s.startedAtIso != null) {
            recordSession(elapsed, completed = false)
        }

        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isFinished = false,
                startedAtIso = null,
                secondsRemaining = it.totalSeconds
            )
        }
    }

    /**
     * Resets everything back to the selected duration.
     */
    fun reset() {
        tickJob?.cancel()
        tickJob = null
        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isFinished = false,
                startedAtIso = null,
                secondsRemaining = it.selectedMinutes * 60,
                totalSeconds = it.selectedMinutes * 60,
                infoMessage = null
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    // ---------------- Internals ----------------

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0) {
                delay(1000L)
                _uiState.update {
                    it.copy(secondsRemaining = (it.secondsRemaining - 1).coerceAtLeast(0))
                }
            }
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        val s = _uiState.value
        val duration = s.totalSeconds

        recordSession(duration, completed = true)

        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isFinished = true,
                startedAtIso = null
            )
        }
    }

    private fun recordSession(elapsedSeconds: Int, completed: Boolean) {
        val s = _uiState.value
        val startIso = s.startedAtIso ?: return
        val endIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        viewModelScope.launch {
            studySessionRepository.recordCompletedSession(
                userId = tokenStorage.getUserId(),
                moduleLocalId = s.selectedModuleLocalId,
                startedAt = startIso,
                endedAt = endIso,
                durationSeconds = elapsedSeconds
            )
            _uiState.update {
                it.copy(
                    infoMessage = "Session saved (${elapsedSeconds / 60} min)"
                )
            }
        }
    }

    companion object {
        /** Sessions shorter than this are discarded. */
        const val MIN_RECORDABLE_SECONDS = 60
    }
}

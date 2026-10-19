package com.zensa.app.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.AchievementRepository
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.StudySessionRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.di.ServiceLocator
import com.zensa.app.domain.model.AchievementSnapshot
import com.zensa.app.domain.usecase.AchievementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AchievementsViewModel
 *
 * Loads the raw data (assignments, subtasks, sessions), asks
 * AchievementsUseCase to compute the snapshot, then persists any
 * newly unlocked badges back to Room so their unlock dates stick.
 *
 * No reactive Flow is needed here — the achievements screen is
 * not a live view like the dashboard; it can be recomputed on
 * every visit.
 */
class AchievementsViewModel(
    private val achievementRepository: AchievementRepository =
        ServiceLocator.achievementRepository,
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val subtaskRepository: SubtaskRepository =
        ServiceLocator.subtaskRepository,
    private val studySessionRepository: StudySessionRepository =
        ServiceLocator.studySessionRepository,
    private val tokenStorage: TokenStorage =
        ServiceLocator.tokenStorage(),
    private val useCase: AchievementsUseCase = AchievementsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userId = tokenStorage.getUserId()

            // ---- Load raw data ----
            val assignments = assignmentRepository.observeAssignments(userId)
                .let { flow -> first(flow) }
            val sessions = studySessionRepository.getForUser(userId)
            val subtasks = assignments.flatMap { a ->
                subtaskRepository.getForAssignment(a.localId)
            }

            // ---- Load currently-stored unlock dates ----
            val unlockedAtMap = achievementRepository.unlockedAtMap()

            // ---- Compute ----
            val snapshot: AchievementSnapshot = useCase.compute(
                assignments = assignments,
                subtasks = subtasks,
                sessions = sessions,
                unlockedAtMap = unlockedAtMap
            )

            // ---- Persist newly unlocked badges ----
            val newlyUnlockedIds = snapshot.achievements
                .filter { it.unlocked && it.unlockedAt == null }
                .map { it.achievement.id }
            if (newlyUnlockedIds.isNotEmpty()) {
                achievementRepository.recordAll(newlyUnlockedIds)
            }

            // ---- Re-read with updated unlock dates ----
            val refreshedMap = achievementRepository.unlockedAtMap()
            val finalSnapshot = useCase.compute(
                assignments = assignments,
                subtasks = subtasks,
                sessions = sessions,
                unlockedAtMap = refreshedMap
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    snapshot = finalSnapshot,
                    error = null
                )
            }
        }
    }

    private suspend fun <T> first(flow: kotlinx.coroutines.flow.Flow<T>): T {
        var value: T? = null
        flow.collect { v ->
            value = v
            throw StopCollect
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private object StopCollect : Throwable()
}

data class AchievementsUiState(
    val isLoading: Boolean = false,
    val snapshot: AchievementSnapshot? = null,
    val error: String? = null
)

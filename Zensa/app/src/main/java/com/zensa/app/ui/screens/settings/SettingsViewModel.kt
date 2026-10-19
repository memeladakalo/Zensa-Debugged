package com.zensa.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.SettingsRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SettingsViewModel
 *
 * Owns everything the Settings screen needs:
 *   - Theme choice
 *   - Notification toggle
 *   - Default study duration
 *   - Default priority
 *   - Profile edit (name)
 *   - Password change
 *   - "Sync now" action
 *
 * Preference values are read live from DataStore, so the screen
 * always reflects the true stored state.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository =
        ServiceLocator.settingsRepository,
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val moduleRepository: ModuleRepository =
        ServiceLocator.moduleRepository,
    private val tokenStorage: TokenStorage =
        ServiceLocator.tokenStorage()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                name = tokenStorage.getUserName().orEmpty(),
                email = tokenStorage.getUserEmail().orEmpty()
            )
        }
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            settingsRepository.theme.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            settingsRepository.notificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultStudyDuration.collect { minutes ->
                _uiState.update { it.copy(defaultStudyDuration = minutes) }
            }
        }
        viewModelScope.launch {
            settingsRepository.defaultPriority.collect { priority ->
                _uiState.update { it.copy(defaultPriority = priority) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lastSyncAt.collect { last ->
                _uiState.update { it.copy(lastSyncAt = last) }
            }
        }
    }

    // ---------------- Preference actions ----------------

    fun onThemeChange(value: String) {
        viewModelScope.launch { settingsRepository.setTheme(value) }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun onDefaultDurationChange(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDefaultStudyDuration(minutes) }
    }

    fun onDefaultPriorityChange(priority: String) {
        viewModelScope.launch { settingsRepository.setDefaultPriority(priority) }
    }

    // ---------------- Profile actions ----------------

    fun onEditNameChange(value: String) {
        _uiState.update { it.copy(editName = value, editNameError = null, editNameSuccess = false) }
    }

    fun onEditNameToggle(show: Boolean) {
        _uiState.update {
            it.copy(
                showEditName = show,
                editName = if (show) it.name else "",
                editNameError = null,
                editNameSuccess = false
            )
        }
    }

    fun saveName() {
        val s = _uiState.value
        if (s.editName.isBlank()) {
            _uiState.update { it.copy(editNameError = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingName = true) }
            when (val r = settingsRepository.updateName(s.editName.trim())) {
                is ApiResult.Success -> {
                    tokenStorage.saveUser(
                        tokenStorage.getUserId().orEmpty(),
                        s.editName.trim(),
                        tokenStorage.getUserEmail().orEmpty()
                    )
                    _uiState.update {
                        it.copy(
                            name = s.editName.trim(),
                            isSavingName = false,
                            showEditName = false,
                            editNameSuccess = true
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSavingName = false, editNameError = r.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    // ---------------- Password actions ----------------

    fun onPasswordToggle(show: Boolean) {
        _uiState.update {
            it.copy(
                showChangePassword = show,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                passwordError = null,
                passwordSuccess = false
            )
        }
    }

    fun onCurrentPasswordChange(v: String) =
        _uiState.update { it.copy(currentPassword = v, passwordError = null) }

    fun onNewPasswordChange(v: String) =
        _uiState.update { it.copy(newPassword = v, passwordError = null) }

    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, passwordError = null) }

    fun savePassword() {
        val s = _uiState.value
        val error = when {
            s.currentPassword.isBlank() -> "Enter your current password"
            s.newPassword.length < 8 -> "New password must be at least 8 characters"
            s.newPassword != s.confirmPassword -> "New passwords do not match"
            s.newPassword == s.currentPassword -> "New password must differ from current"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(passwordError = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPassword = true, passwordError = null) }
            when (val r = settingsRepository.changePassword(
                s.currentPassword, s.newPassword
            )) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSavingPassword = false,
                        showChangePassword = false,
                        passwordSuccess = true,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = ""
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSavingPassword = false, passwordError = r.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    // ---------------- Sync ----------------

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }

            val m = moduleRepository.refresh(tokenStorage.getUserId())
            val a = assignmentRepository.refresh()

            val ok = m is ApiResult.Success && a is ApiResult.Success
            if (ok) {
                settingsRepository.markSyncedNow()
                _uiState.update {
                    it.copy(isSyncing = false, syncMessage = "Sync complete")
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = (m as? ApiResult.Error)?.message
                            ?: (a as? ApiResult.Error)?.message
                            ?: "Sync failed"
                    )
                }
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    // ---------------- Logout ----------------

    fun logout() {
        tokenStorage.clear()
    }
}

data class SettingsUiState(
    // Profile
    val name: String = "",
    val email: String = "",
    val showEditName: Boolean = false,
    val editName: String = "",
    val editNameError: String? = null,
    val editNameSuccess: Boolean = false,
    val isSavingName: Boolean = false,

    // Password
    val showChangePassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val passwordSuccess: Boolean = false,
    val isSavingPassword: Boolean = false,

    // Preferences
    val theme: String = UserPreferences.THEME_SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultStudyDuration: Int = 25,
    val defaultPriority: String = UserPreferences.PRIORITY_MEDIUM,

    // Sync
    val isSyncing: Boolean = false,
    val lastSyncAt: String? = null,
    val syncMessage: String? = null
)

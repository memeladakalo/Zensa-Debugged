package com.zensa.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.core.util.ValidationUtils
import com.zensa.app.data.repository.AuthRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LoginViewModel
 *
 * Owns the state of the Login screen. The Compose UI never changes
 * state directly — it only calls methods here, and reads the state
 * from [uiState].
 *
 * Real authentication:
 *   - Validation runs locally (ValidationUtils).
 *   - On success, calls AuthRepository.login(...) which sends a real
 *     POST /api/auth/login request and stores the returned JWT.
 *
 * The `authRepository` parameter has a default so the ViewModel can
 * be constructed normally in production, and injected with a fake
 * in unit tests.
 */
class LoginViewModelTest(
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    // ---------- State ----------

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ---------- Events from the UI ----------

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                generalError = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * Called when the user taps Login.
     *
     * Flow:
     *   1. Validate fields.
     *   2. If invalid → set field errors, stop. The repository is
     *      never called.
     *   3. Otherwise show loading, call authRepository.login(...).
     *   4. On success → invoke [onSuccess] (UI navigates away).
     *   5. On failure → set generalError.
     */
    fun login(onSuccess: () -> Unit) {
        val current = _uiState.value

        // --- Step 1 & 2: validation ---
        val emailError = ValidationUtils.emailError(current.email)
        val passwordError = ValidationUtils.passwordError(current.password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        // --- Step 3: perform real login ---
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.login(
                email = current.email.trim(),
                password = current.password
            )

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message
                        )
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}

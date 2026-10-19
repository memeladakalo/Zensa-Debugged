package com.zensa.app.ui.screens.login

/**
 * LoginUiState
 *
 * A single immutable data class representing EVERYTHING the Login
 * screen needs to render itself. The ViewModel exposes one of these
 * via StateFlow. The Compose UI just reads it.
 *
 * This is the modern "single source of truth" pattern recommended
 * by Android. It also makes the screen trivially testable.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,

    // Field-level validation errors (shown under each field)
    val emailError: String? = null,
    val passwordError: String? = null,

    // Whole-screen error (shown above the button, e.g. "Invalid credentials")
    val generalError: String? = null,

    // True while the login request is in progress
    val isLoading: Boolean = false
) {
    /** True when every field has a valid value. */
    val isFormValid: Boolean
        get() = email.isNotBlank() &&
                password.isNotBlank() &&
                emailError == null &&
                passwordError == null
}

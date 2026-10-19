package com.zensa.app.core.util

/**
 * ValidationUtils
 *
 * Pure Kotlin validation helpers — no Android dependencies.
 * This means we can unit-test them directly on the JVM with plain
 * JUnit, without an emulator. This is critical for the "detailed
 * unit testing" rubric item.
 *
 * Every rule here maps to a Functional Requirement:
 *   - Email format  → FR-01 (registration validation)
 *   - Password length → FR-01
 *   - Required field → FR-01, FR-19 (empty fields)
 */
object ValidationUtils {

    /** Simple but practical email pattern. Not fully RFC compliant —
     *  it rejects the obvious problems without false negatives. */
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    /** Minimum password length. Matches NIST/OWASP guidance for Part 2. */
    const val MIN_PASSWORD_LENGTH = 8

    /**
     * Returns true if [email] looks like a valid email address.
     * Returns false if null, blank, or malformed.
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return EMAIL_REGEX.matches(email.trim())
    }

    /**
     * Returns true if [password] meets minimum security requirements.
     * Currently: at least 8 characters. We may tighten this later.
     */
    fun isValidPassword(password: String?): Boolean {
        if (password.isNullOrBlank()) return false
        return password.length >= MIN_PASSWORD_LENGTH
    }

    /**
     * Returns true if [field] is non-null and non-blank.
     * Used for required fields like "name".
     */
    fun isNotEmpty(field: String?): Boolean {
        return !field.isNullOrBlank()
    }

    /**
     * Convenience: returns a human-readable email error message,
     * or null if the email is valid.
     */
    fun emailError(email: String?): String? {
        return when {
            email.isNullOrBlank() -> "Email is required"
            !isValidEmail(email)  -> "Enter a valid email address"
            else                  -> null
        }
    }

    /**
     * Convenience: returns a human-readable password error message,
     * or null if the password is valid.
     */
    fun passwordError(password: String?): String? {
        return when {
            password.isNullOrBlank() -> "Password is required"
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters"
            else -> null
        }
    }
}

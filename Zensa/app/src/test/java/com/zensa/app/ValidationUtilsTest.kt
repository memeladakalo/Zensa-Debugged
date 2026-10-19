package com.zensa.app

import com.zensa.app.core.util.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ValidationUtilsTest
 *
 * Unit tests covering the validation helpers used by the
 * Login and Register screens.
 *
 * Runs on the JVM — no emulator needed.
 */
class ValidationUtilsTest {

    // --------------------------------------------------------------
    // Email format
    // --------------------------------------------------------------

    @Test
    fun validEmailIsAccepted() {
        assertTrue(ValidationUtils.isValidEmail("student@zensa.app"))
        assertTrue(ValidationUtils.isValidEmail("first.last@university.edu.za"))
        assertTrue(ValidationUtils.isValidEmail("a+b@c.co"))
    }

    @Test
    fun emailWithoutAtIsRejected() {
        assertFalse(ValidationUtils.isValidEmail("student.zensa.app"))
    }

    @Test
    fun emailWithoutDomainIsRejected() {
        assertFalse(ValidationUtils.isValidEmail("student@"))
    }

    @Test
    fun emailWithoutTldIsRejected() {
        assertFalse(ValidationUtils.isValidEmail("student@zensa"))
    }

    @Test
    fun emailWithWhitespaceAroundIsTrimmedAndAccepted() {
        assertTrue(
            ValidationUtils.isValidEmail("  student@zensa.app  ")
        )
    }

    @Test
    fun nullEmailIsRejected() {
        assertFalse(ValidationUtils.isValidEmail(null))
    }

    @Test
    fun blankEmailIsRejected() {
        assertFalse(ValidationUtils.isValidEmail(""))
        assertFalse(ValidationUtils.isValidEmail("   "))
    }

    // --------------------------------------------------------------
    // Password length
    // --------------------------------------------------------------

    @Test
    fun passwordExactlyAtMinimumLengthIsAccepted() {
        assertTrue(
            ValidationUtils.isValidPassword("abcdefgh")
        )
    }

    @Test
    fun passwordOneBelowMinimumIsRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("abcdefg")
        )
    }

    @Test
    fun passwordAboveMinimumIsAccepted() {
        assertTrue(
            ValidationUtils.isValidPassword("averylongpassword")
        )
    }

    @Test
    fun nullPasswordIsRejected() {
        assertFalse(
            ValidationUtils.isValidPassword(null)
        )
    }

    @Test
    fun blankPasswordIsRejected() {
        assertFalse(
            ValidationUtils.isValidPassword("")
        )
    }

    // --------------------------------------------------------------
    // Required field
    // --------------------------------------------------------------

    @Test
    fun nonEmptyFieldPassesIsNotEmpty() {
        assertTrue(
            ValidationUtils.isNotEmpty("Ada")
        )
    }

    @Test
    fun blankFieldFailsIsNotEmpty() {
        assertFalse(
            ValidationUtils.isNotEmpty("")
        )
        assertFalse(
            ValidationUtils.isNotEmpty("    ")
        )
        assertFalse(
            ValidationUtils.isNotEmpty(null)
        )
    }

    // --------------------------------------------------------------
    // emailError messages
    // --------------------------------------------------------------

    @Test
    fun emailErrorIsNullForValidEmail() {
        assertNull(
            ValidationUtils.emailError("student@zensa.app")
        )
    }

    @Test
    fun emailErrorReportsMissingEmail() {
        assertNotNull(
            ValidationUtils.emailError(null)
        )
        assertNotNull(
            ValidationUtils.emailError("")
        )
        assertTrue(
            ValidationUtils.emailError("")!!
                .contains("required", ignoreCase = true)
        )
    }

    @Test
    fun emailErrorReportsMalformedEmail() {
        val msg = ValidationUtils.emailError("not-an-email")

        assertNotNull(msg)
        assertTrue(
            msg!!.contains("valid", ignoreCase = true)
        )
    }

    // --------------------------------------------------------------
    // passwordError messages
    // --------------------------------------------------------------

    @Test
    fun passwordErrorIsNullForValidPassword() {
        assertNull(
            ValidationUtils.passwordError("password123")
        )
    }

    @Test
    fun passwordErrorReportsShortPassword() {
        val msg = ValidationUtils.passwordError("abc")

        assertNotNull(msg)
        assertTrue(
            msg!!.contains("8", ignoreCase = false)
        )
    }

    @Test
    fun passwordErrorReportsMissingPassword() {
        val msg = ValidationUtils.passwordError("")

        assertNotNull(msg)
        assertEquals(
            true,
            msg!!.contains("required", ignoreCase = true)
        )
    }
}
package com.zensa.app.core.constants

/**
 * AppConstants
 *
 * Central place for constants used across Zensa.
 * Keeping them here (instead of scattering "magic strings" in code)
 * makes them easy to change later and easy to unit-test against.
 */
object AppConstants {

    /** Shown on the Splash screen. */
    const val APP_NAME = "Zensa"

    /** Zensa tagline from the Planning & Design document. */
    const val APP_TAGLINE = "Calm, focused studying."

    /**
     * Base URL of the Zensa REST API.
     *
     * During development this points to the Spring Boot server running
     * on your PC. The Android emulator reaches your PC's localhost via
     * the special address 10.0.2.2.
     *
     * We are NOT using this yet — it is here so that when we build the
     * network layer in a later step, we just reference this constant.
     */
    const val API_BASE_URL_DEV = "http://10.0.2.2:8080/"

    /**
     * Version of the local Room database schema.
     * Bump this whenever we change entity definitions.
     */
    const val DATABASE_VERSION = 6

    /** Name of the local Room database file. */
    const val DATABASE_NAME = "zensa.db"

    /** Splash screen auto-navigation delay (milliseconds). */
    const val SPLASH_DELAY_MS = 1500L
}

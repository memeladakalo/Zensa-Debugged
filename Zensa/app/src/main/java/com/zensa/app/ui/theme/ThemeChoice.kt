package com.zensa.app.ui.theme

/**
 * ThemeChoice — the resolved theme setting for ZensaTheme.
 *
 * Kept separate from UserPreferences so the UI layer never has to
 * import data-layer constants.
 */
enum class ThemeChoice {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromPreference(value: String?): ThemeChoice = when (value?.uppercase()) {
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            else -> SYSTEM
        }
    }
}

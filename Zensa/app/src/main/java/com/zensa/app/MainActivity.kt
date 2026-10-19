package com.zensa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.di.ServiceLocator
import com.zensa.app.ui.navigation.ZensaNavHost
import com.zensa.app.ui.theme.ThemeChoice
import com.zensa.app.ui.theme.ZensaTheme

/**
 * MainActivity — the single Activity that hosts all Zensa screens.
 *
 * Reads the theme preference from DataStore and passes it into
 * ZensaTheme. If the user picks SYSTEM, we fall through to the
 * device's dark-mode setting.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs: UserPreferences = ServiceLocator.userPreferences()

        setContent {
            val themeValue by prefs.theme.collectAsState(
                initial = UserPreferences.THEME_SYSTEM
            )
            val choice = ThemeChoice.fromPreference(themeValue)
            val dark = when (choice) {
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
            }

            ZensaTheme(darkTheme = dark) {
                ZensaNavHost()
            }
        }
    }
}

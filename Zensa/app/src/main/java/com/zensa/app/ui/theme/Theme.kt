package com.zensa.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Zensa light colour scheme.
 * Used when the device is in light mode (default).
 */
private val ZensaLightColors = lightColorScheme(
    primary          = ZensaPurple,
    onPrimary        = ZensaWhite,
    primaryContainer = ZensaPurpleLight,
    onPrimaryContainer = ZensaPurpleDark,

    secondary        = ZensaLavender,
    onSecondary      = ZensaBlack,
    secondaryContainer = ZensaLavenderLight,
    onSecondaryContainer = ZensaPurpleDark,

    tertiary         = ZensaPink,
    onTertiary       = ZensaBlack,

    background       = ZensaOffWhite,
    onBackground     = ZensaBlack,

    surface          = ZensaWhite,
    onSurface        = ZensaBlack,
    surfaceVariant   = ZensaLavenderLight,
    onSurfaceVariant = ZensaGrey,

    error            = ZensaError,
    onError          = ZensaWhite
)

/**
 * Zensa dark colour scheme.
 * Placeholder for now — full dark support comes with the Settings screen.
 */
private val ZensaDarkColors = darkColorScheme(
    primary          = ZensaPurpleLight,
    onPrimary        = ZensaBlack,
    primaryContainer = ZensaPurpleDark,
    onPrimaryContainer = ZensaWhite,

    secondary        = ZensaLavender,
    onSecondary      = ZensaBlack,

    tertiary         = ZensaPink,
    onTertiary       = ZensaBlack,

    background       = ZensaBlack,
    onBackground     = ZensaWhite,

    surface          = Color(0xFF232025),
    onSurface        = ZensaWhite,

    error            = ZensaError,
    onError          = ZensaWhite
)

/**
 * ZensaTheme — wraps every screen.
 *
 * Currently follows the system dark/light setting.
 * Once the Settings screen exists, the user's choice (stored in
 * DataStore) will override this.
 */
@Composable
fun ZensaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZensaDarkColors else ZensaLightColors

    // Make status-bar icons match the theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = ZensaTypography,
        shapes      = ZensaShapes,
        content     = content
    )
}

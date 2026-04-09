package com.example.earrove.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.remember
import com.example.earrove.data.settings.SettingsRepositoryImpl

private val DarkColorScheme = darkColorScheme(
    primary = PremiumGold,           // Core interactive elements
    onPrimary = PureBlack,           // Text on primary elements
    background = PureBlack,          // App background
    surface = DeepGray,              // Surface for cards, sheets etc.
    onBackground = PureWhite,        // Default text color
    onSurface = PureWhite,           // Text on surfaces
    onSurfaceVariant = PureWhite,    // For less prominent text/icons
    error = WarningOrange,           // For warnings
    onError = PureBlack              // Text on warning elements
)

private val VisualAssistColorScheme = darkColorScheme(
    primary = HighlightYellow,
    onPrimary = PureBlack,
    background = PureBlack,
    surface = PureBlack,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = PureWhite,
    error = WarningOrange,
    onError = PureBlack
)

@Composable
fun EarRoveTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember(context) { SettingsRepositoryImpl(context) }
    val visualAssistEnabled = settingsRepository.isVisualAssistEnabled()
    val colorScheme = if (visualAssistEnabled) VisualAssistColorScheme else DarkColorScheme
    val typography = if (visualAssistEnabled) EarRoveVisualAssistTypography else EarRoveTypography
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

package com.epicnotes.chat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    primary = MessageUserDark,
    onPrimary = Color.White,
    secondaryContainer = MessageAssistantDark,
    onSecondaryContainer = TextPrimaryDark,
    outlineVariant = InputBarDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    tertiary = AccentDark
)

private val LightColorScheme = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    primary = MessageUserLight,
    onPrimary = Color.White,
    secondaryContainer = MessageAssistantLight,
    onSecondaryContainer = TextPrimaryLight,
    outlineVariant = InputBarLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    tertiary = AccentLight
)

/**
 * Main theme for the Chat app.
 * Uses Material 3 design system.
 */
@Composable
fun ChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

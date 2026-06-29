package com.cunffy.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Material You theming. With [dynamicColor] on (Android 12+) the scheme is derived from the
 * wallpaper by the system; otherwise [seedColor] (an accent preset or a color extracted from
 * the wallpaper) is used. [darkTheme] is resolved from the user's theme-mode setting upstream.
 */
@Composable
fun LauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Int = 0xFF4CAF50.toInt(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        else -> accentScheme(Color(seedColor), darkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LauncherTypography,
        content = content,
    )
}

private fun accentScheme(primary: Color, dark: Boolean) =
    if (dark) {
        darkColorScheme(primary = primary, secondary = primary, tertiary = primary)
    } else {
        lightColorScheme(primary = primary, secondary = primary, tertiary = primary)
    }

package com.cunffy.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FallbackDark = darkColorScheme(primary = GreenPrimary, background = Surface, surface = Surface)
private val FallbackLight = lightColorScheme(primary = GreenDark, background = SurfaceLight, surface = SurfaceLight)

/**
 * Material You theming. With [dynamicColor] on (Android 12+) the scheme is derived from the
 * wallpaper; otherwise a fixed fallback palette is used. [darkTheme] is resolved from the
 * user's theme-mode setting upstream.
 */
@Composable
fun LauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LauncherTypography,
        content = content,
    )
}

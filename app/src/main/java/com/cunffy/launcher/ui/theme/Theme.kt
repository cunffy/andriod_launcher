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
import com.cunffy.launcher.data.prefs.AccentPreset

/**
 * Material You theming. With [dynamicColor] on (Android 12+) the scheme is derived from the
 * wallpaper; otherwise the chosen [accent] preset palette is used. [darkTheme] is resolved from
 * the user's theme-mode setting upstream.
 */
@Composable
fun LauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accent: AccentPreset = AccentPreset.GREEN,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        else -> accentScheme(accent, darkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LauncherTypography,
        content = content,
    )
}

private fun accentScheme(accent: AccentPreset, dark: Boolean) = run {
    val primary = when (accent) {
        AccentPreset.GREEN -> Color(0xFF4CAF50)
        AccentPreset.BLUE -> Color(0xFF2196F3)
        AccentPreset.PURPLE -> Color(0xFF9C27B0)
        AccentPreset.ORANGE -> Color(0xFFFF9800)
        AccentPreset.RED -> Color(0xFFF44336)
        AccentPreset.TEAL -> Color(0xFF009688)
    }
    if (dark) {
        darkColorScheme(primary = primary, secondary = primary, tertiary = primary)
    } else {
        lightColorScheme(primary = primary, secondary = primary, tertiary = primary)
    }
}

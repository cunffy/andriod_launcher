package com.cunffy.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.prefs.LauncherSettings
import com.cunffy.launcher.data.prefs.ThemeMode
import com.cunffy.launcher.data.theme.WallpaperColorProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferences: LauncherPreferences,
    wallpaperColorProvider: WallpaperColorProvider,
) : ViewModel() {
    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())
    val wallpaperColor: StateFlow<Int?> = wallpaperColorProvider.primaryColor
}

/** Applies [LauncherTheme] using the user's theme-mode and accent settings. */
@Composable
fun LauncherThemeGate(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val wallpaperColor by viewModel.wallpaperColor.collectAsStateWithLifecycle()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    LauncherTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.dynamicColor,
        seedColor = accentSeed(settings, wallpaperColor),
        content = content,
    )
}

/**
 * The accent color to use when dynamic color is off: the wallpaper's reported color when that
 * option is on and available, otherwise the chosen preset.
 */
fun accentSeed(settings: LauncherSettings, wallpaperColor: Int?): Int =
    if (settings.accentFromWallpaper && wallpaperColor != null) {
        wallpaperColor
    } else {
        settings.accentPreset.argb()
    }

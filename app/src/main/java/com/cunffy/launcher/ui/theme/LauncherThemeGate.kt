package com.cunffy.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.prefs.LauncherSettings
import com.cunffy.launcher.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferences: LauncherPreferences,
) : ViewModel() {
    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())
}

/** Applies [LauncherTheme] using the user's theme-mode and dynamic-color settings. */
@Composable
fun LauncherThemeGate(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    LauncherTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.dynamicColor,
        accent = settings.accentPreset,
        content = content,
    )
}

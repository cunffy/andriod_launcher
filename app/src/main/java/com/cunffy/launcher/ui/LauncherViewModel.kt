package com.cunffy.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.prefs.LauncherSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    preferences: LauncherPreferences,
) : ViewModel() {
    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())
}

package com.cunffy.launcher.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.prefs.LauncherPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: LauncherPreferences,
) : ViewModel() {

    fun complete() {
        viewModelScope.launch { preferences.setOnboardingComplete(true) }
    }
}

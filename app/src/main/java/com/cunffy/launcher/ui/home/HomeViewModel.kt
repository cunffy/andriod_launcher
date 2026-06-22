package com.cunffy.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.apps.AppRepository
import com.cunffy.launcher.data.prefs.LauncherPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    preferences: LauncherPreferences,
) : ViewModel() {

    /**
     * Apps shown in the dock. If the user has pinned a dock we resolve those component
     * strings; otherwise we seed a sensible default from the first few installed apps so
     * the home screen is never empty on first run.
     */
    val dockApps = combine(appRepository.apps, preferences.dockComponents) { apps, pinned ->
        if (pinned.isEmpty()) {
            apps.take(DEFAULT_DOCK_SIZE)
        } else {
            pinned.mapNotNull { key -> apps.firstOrNull { it.componentName.flattenToString() == key } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun launch(app: AppInfo) = appRepository.launch(app)

    private companion object {
        const val DEFAULT_DOCK_SIZE = 5
    }
}

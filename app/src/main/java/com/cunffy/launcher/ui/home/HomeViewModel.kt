package com.cunffy.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.apps.AppCatalog
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.db.entities.HomeItemEntity
import com.cunffy.launcher.data.home.HomeEntry
import com.cunffy.launcher.data.home.HomeLayoutRepository
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.notifications.NotificationBadgeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appCatalog: AppCatalog,
    private val homeLayoutRepository: HomeLayoutRepository,
    private val preferences: LauncherPreferences,
    badgeStore: NotificationBadgeStore,
) : ViewModel() {

    val dockApps = combine(appCatalog.visibleApps, preferences.dockComponents) { apps, pinned ->
        if (pinned.isEmpty()) {
            apps.take(DEFAULT_DOCK_SIZE)
        } else {
            pinned.mapNotNull { key -> apps.firstOrNull { it.componentKey == key } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val desktop = homeLayoutRepository.desktop
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Per-package notification counts, or empty when badges are disabled. */
    val badgeCounts = combine(badgeStore.counts, preferences.settings) { counts, settings ->
        if (settings.badgesEnabled) counts else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _editMode = MutableStateFlow(false)
    val editMode = _editMode.asStateFlow()

    init {
        viewModelScope.launch { homeLayoutRepository.seedDefaultIfEmpty() }
    }

    fun setEditMode(enabled: Boolean) { _editMode.value = enabled }

    fun launch(app: AppInfo) = appCatalog.launch(app)

    fun moveItem(item: HomeItemEntity, cellX: Int, cellY: Int) = viewModelScope.launch {
        homeLayoutRepository.moveItem(item, cellX, cellY)
    }

    fun removeItem(entry: HomeEntry) = viewModelScope.launch {
        homeLayoutRepository.removeItem(entry.item)
    }

    fun mergeIntoFolder(dragged: HomeItemEntity, target: HomeItemEntity) = viewModelScope.launch {
        homeLayoutRepository.mergeIntoFolder(dragged, target)
    }

    fun addWidget(widgetId: Int, spanX: Int, spanY: Int, cellX: Int, cellY: Int, page: Int) =
        viewModelScope.launch {
            homeLayoutRepository.addWidget(widgetId, spanX, spanY, cellX, cellY, page)
        }

    fun renameFolder(folderId: Long, title: String) = viewModelScope.launch {
        homeLayoutRepository.renameFolder(folderId, title)
    }

    private companion object {
        const val DEFAULT_DOCK_SIZE = 5
    }
}

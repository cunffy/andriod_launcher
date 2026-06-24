package com.cunffy.launcher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.apps.AppCatalog
import com.cunffy.launcher.data.apps.AppCategory
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.apps.DockRepository
import com.cunffy.launcher.data.customization.CustomizationRepository
import com.cunffy.launcher.data.home.HomeLayoutRepository
import com.cunffy.launcher.data.prefs.DrawerSortMode
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.prefs.LauncherSettings
import com.cunffy.launcher.data.usage.AppUsage
import com.cunffy.launcher.data.usage.AppUsageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appCatalog: AppCatalog,
    private val customizationRepository: CustomizationRepository,
    private val homeLayoutRepository: HomeLayoutRepository,
    private val dockRepository: DockRepository,
    private val usageProvider: AppUsageProvider,
    private val preferences: LauncherPreferences,
) : ViewModel() {

    private val allApps = appCatalog.visibleApps

    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())

    val dockKeys = dockRepository.dockKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _usage = MutableStateFlow<Map<String, AppUsage>>(emptyMap())

    val categories = allApps
        .map { apps ->
            val present = apps.mapTo(mutableSetOf()) { it.category }
            AppCategory.sidebarOrder.filter { it == AppCategory.ALL || it in present }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(AppCategory.ALL))

    val visibleApps = combine(
        allApps,
        _selectedCategory,
        preferences.settings,
        _usage,
    ) { apps, category, settings, usage ->
        val inCategory =
            if (category == AppCategory.ALL) apps else apps.filter { it.category == category }
        when (settings.drawerSort) {
            DrawerSortMode.ALPHABETICAL -> inCategory
            DrawerSortMode.RECENT ->
                inCategory.sortedByDescending { usage[it.packageName]?.lastUsed ?: 0L }
            DrawerSortMode.MOST_USED ->
                inCategory.sortedByDescending { usage[it.packageName]?.totalTimeMs ?: 0L }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun hasUsagePermission(): Boolean = usageProvider.hasPermission()

    fun usageAccessIntent() = usageProvider.usageAccessSettingsIntent()

    /** Re-read usage stats (cheap; call when the drawer opens so the order stays fresh). */
    fun refreshUsage() = viewModelScope.launch {
        _usage.value = withContext(Dispatchers.IO) { usageProvider.usageByPackage() }
    }

    fun setSortMode(mode: DrawerSortMode) = viewModelScope.launch {
        preferences.setDrawerSort(mode)
        refreshUsage()
    }

    fun selectCategory(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun launch(app: AppInfo) = appCatalog.launch(app)

    fun setHidden(app: AppInfo, hidden: Boolean) = viewModelScope.launch {
        customizationRepository.setHidden(app.componentKey, hidden)
    }

    fun setLocked(app: AppInfo, locked: Boolean) = viewModelScope.launch {
        customizationRepository.setLocked(app.componentKey, locked)
    }

    fun setLabel(app: AppInfo, label: String?) = viewModelScope.launch {
        customizationRepository.setLabel(app.componentKey, label)
    }

    fun setCategoryOverride(app: AppInfo, category: AppCategory?) = viewModelScope.launch {
        customizationRepository.setCategoryOverride(app.componentKey, category?.name)
    }

    fun addToHome(app: AppInfo) = viewModelScope.launch {
        homeLayoutRepository.addApp(app.componentKey, cellX = 0, cellY = 0)
    }

    fun isInDock(app: AppInfo): Boolean = dockKeys.value.contains(app.componentKey)

    fun toggleDock(app: AppInfo) = viewModelScope.launch {
        if (isInDock(app)) dockRepository.remove(app) else dockRepository.add(app)
    }
}

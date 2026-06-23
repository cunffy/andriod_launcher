package com.cunffy.launcher.data.apps

import com.cunffy.launcher.data.prefs.LauncherPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the dock's ordered app list. Falls back to [DockDefaults] until the user customizes it;
 * once edited, the explicit list is persisted so it stays put.
 */
@Singleton
class DockRepository @Inject constructor(
    private val preferences: LauncherPreferences,
    private val dockDefaults: DockDefaults,
    private val appCatalog: AppCatalog,
) {
    /** Effective dock component keys (persisted list, or defaults when none set). */
    val dockKeys: Flow<List<String>> =
        combine(preferences.dockComponents, appCatalog.visibleApps) { pinned, apps ->
            if (pinned.isNotEmpty()) pinned else dockDefaults.defaultKeys(apps)
        }

    private suspend fun current(): List<String> {
        val pinned = preferences.dockComponents.first()
        return if (pinned.isNotEmpty()) pinned else dockDefaults.defaultKeys(appCatalog.visibleApps.value)
    }

    suspend fun add(app: AppInfo) {
        val cur = current()
        if (app.componentKey !in cur) preferences.setDockComponents(cur + app.componentKey)
    }

    suspend fun remove(app: AppInfo) {
        preferences.setDockComponents(current() - app.componentKey)
    }
}

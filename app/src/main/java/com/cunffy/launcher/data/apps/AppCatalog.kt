package com.cunffy.launcher.data.apps

import com.cunffy.launcher.data.customization.CustomizationRepository
import com.cunffy.launcher.data.icons.IconResolver
import com.cunffy.launcher.data.prefs.LauncherPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for the *effective* app list shown across the launcher: raw
 * installed apps from [AppRepository] merged with user customizations (label, icon pack,
 * hidden/locked, category override) and the global icon/theme settings.
 */
@Singleton
class AppCatalog @Inject constructor(
    private val appRepository: AppRepository,
    customizationRepository: CustomizationRepository,
    preferences: LauncherPreferences,
    private val iconResolver: IconResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every app including hidden ones (used by the hidden-apps manager). */
    val allApps: StateFlow<List<AppInfo>> = combine(
        appRepository.apps,
        customizationRepository.customizations,
        preferences.settings,
    ) { apps, customs, settings ->
        apps.map { base ->
            val c = customs[base.componentKey]
            val category = c?.categoryOverride
                ?.let { runCatching { AppCategory.valueOf(it) }.getOrNull() }
                ?: base.category
            val pack = c?.iconPackPackage ?: settings.iconPackPackage
            base.copy(
                label = c?.customLabel ?: base.label,
                category = category,
                icon = iconResolver.resolve(base.componentName, base.icon, pack, settings.themedIcons),
                hidden = c?.hidden == true,
                locked = c?.locked == true,
            )
        }.sortedBy { it.label.lowercase() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Apps shown in the drawer/search (hidden apps removed). */
    val visibleApps: StateFlow<List<AppInfo>> = allApps
        .map { list -> list.filterNot { it.hidden } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun launch(app: AppInfo) = appRepository.launch(app)
}

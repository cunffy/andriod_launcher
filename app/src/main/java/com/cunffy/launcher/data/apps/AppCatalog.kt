package com.cunffy.launcher.data.apps

import android.content.Context
import com.cunffy.launcher.data.customization.CustomizationRepository
import com.cunffy.launcher.data.icons.IconResolver
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.theme.WallpaperColorProvider
import com.cunffy.launcher.ui.components.IconCache
import com.cunffy.launcher.ui.theme.argb
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    @ApplicationContext context: Context,
    private val appRepository: AppRepository,
    customizationRepository: CustomizationRepository,
    preferences: LauncherPreferences,
    wallpaperColorProvider: WallpaperColorProvider,
    private val iconResolver: IconResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Point the icon cache at disk so renders survive a process death (fast cold start).
        IconCache.initialize(context.cacheDir)
    }

    /** Every app including hidden ones (used by the hidden-apps manager). */
    val allApps: StateFlow<List<AppInfo>> = combine(
        appRepository.apps,
        customizationRepository.customizations,
        preferences.settings,
        wallpaperColorProvider.primaryColor,
    ) { apps, customs, settings, wallpaperColor ->
        // Themed-icon tint: our accent (preset or wallpaper color) when not using system dynamic
        // color; null means fall back to the system Material You accent.
        val themedTint: Int? = if (settings.themedIcons && !settings.dynamicColor) {
            if (settings.accentFromWallpaper) {
                wallpaperColor ?: settings.accentPreset.argb()
            } else {
                settings.accentPreset.argb()
            }
        } else {
            null
        }
        apps.map { base ->
            val c = customs[base.componentKey]
            val category = c?.categoryOverride
                ?.let { runCatching { AppCategory.valueOf(it) }.getOrNull() }
                ?: base.category
            val pack = c?.iconPackPackage ?: settings.iconPackPackage
            val iconKey =
                "${base.componentKey}|${pack ?: ""}|${settings.themedIcons}|${settings.iconShape}" +
                    "|${themedTint ?: "sys"}"
            // If this exact icon is already rendered on disk, skip the (expensive) re-masking —
            // the cached bitmap will be served by iconKey when the tile is drawn.
            val icon = if (IconCache.hasOnDisk(iconKey)) {
                base.icon
            } else {
                iconResolver.resolve(
                    base.componentName, base.icon, pack, settings.themedIcons, settings.iconShape,
                    themedTint,
                )
            }
            base.copy(
                label = c?.customLabel ?: base.label,
                category = category,
                icon = icon,
                hidden = c?.hidden == true,
                locked = c?.locked == true,
                iconKey = iconKey,
            )
        }.sortedBy { it.label.lowercase() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        // Pre-rasterize icons off the main thread so the drawer scrolls smoothly on first open.
        allApps.onEach { list -> list.forEach { IconCache.warm(it.iconKey, it.icon) } }
            .launchIn(scope)
    }

    /** Apps shown in the drawer/search (hidden apps removed). */
    val visibleApps: StateFlow<List<AppInfo>> = allApps
        .map { list -> list.filterNot { it.hidden } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun launch(app: AppInfo) = appRepository.launch(app)
}

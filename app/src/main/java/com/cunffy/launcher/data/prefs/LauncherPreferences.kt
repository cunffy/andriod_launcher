package com.cunffy.launcher.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cunffy.launcher.gesture.GestureAction
import com.cunffy.launcher.gesture.GestureSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Mask applied to every app icon. */
enum class IconShape { CIRCLE, SQUIRCLE, ROUNDED_SQUARE, SQUARE }

/** Snapshot of all simple launcher settings, derived from DataStore. */
data class LauncherSettings(
    val dockComponents: List<String> = emptyList(),
    val themedIcons: Boolean = false,
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconPackPackage: String? = null,
    val badgesEnabled: Boolean = true,
    val updateUrl: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    /** Dim overlay over the wallpaper, 0–100 %. */
    val wallpaperDim: Int = 0,
    val iconSizeDp: Int = 52,
    val showDrawerLabels: Boolean = true,
    val searchAutoFocus: Boolean = false,
    val clock24h: Boolean = false,
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val homePageCount: Int = 1,
    val gestures: Map<GestureSlot, GestureAction> =
        GestureSlot.entries.associateWith { it.defaultAction },
)

/**
 * Lightweight persisted launcher settings (everything that isn't relational lives here;
 * apps/folders/home-layout live in Room).
 */
@Singleton
class LauncherPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dockKey = stringPreferencesKey("dock_components")
    private val themedIconsKey = booleanPreferencesKey("themed_icons")
    private val iconShapeKey = stringPreferencesKey("icon_shape")
    private val iconPackKey = stringPreferencesKey("icon_pack")
    private val badgesKey = booleanPreferencesKey("badges_enabled")
    private val updateUrlKey = stringPreferencesKey("update_url")
    private val onboardedKey = booleanPreferencesKey("onboarding_complete")
    private val homeResetKey = booleanPreferencesKey("home_layout_reset_v5")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val wallpaperDimKey = intPreferencesKey("wallpaper_dim")
    private val iconSizeKey = intPreferencesKey("icon_size")
    private val drawerLabelsKey = booleanPreferencesKey("drawer_labels")
    private val searchAutoFocusKey = booleanPreferencesKey("search_autofocus")
    private val clock24hKey = booleanPreferencesKey("clock_24h")
    private val gridColumnsKey = intPreferencesKey("grid_columns")
    private val gridRowsKey = intPreferencesKey("grid_rows")
    private val homePageCountKey = intPreferencesKey("home_page_count")
    private fun gestureKey(slot: GestureSlot) = stringPreferencesKey("gesture_${slot.name}")

    val settings: Flow<LauncherSettings> = context.dataStore.data.map { it.toSettings() }

    val dockComponents: Flow<List<String>> =
        context.dataStore.data.map { it[dockKey].toComponentList() }

    private fun Preferences.toSettings() = LauncherSettings(
        dockComponents = this[dockKey].toComponentList(),
        themedIcons = this[themedIconsKey] ?: false,
        iconShape = runCatching { IconShape.valueOf(this[iconShapeKey] ?: "") }
            .getOrDefault(IconShape.CIRCLE),
        iconPackPackage = this[iconPackKey]?.ifBlank { null },
        badgesEnabled = this[badgesKey] ?: true,
        updateUrl = this[updateUrlKey]?.ifBlank { null },
        themeMode = runCatching { ThemeMode.valueOf(this[themeModeKey] ?: "") }
            .getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = this[dynamicColorKey] ?: true,
        wallpaperDim = this[wallpaperDimKey] ?: 0,
        iconSizeDp = this[iconSizeKey] ?: 52,
        showDrawerLabels = this[drawerLabelsKey] ?: true,
        searchAutoFocus = this[searchAutoFocusKey] ?: false,
        clock24h = this[clock24hKey] ?: false,
        gridColumns = this[gridColumnsKey] ?: 4,
        gridRows = this[gridRowsKey] ?: 5,
        homePageCount = this[homePageCountKey] ?: 1,
        gestures = GestureSlot.entries.associateWith { slot ->
            GestureAction.fromName(this[gestureKey(slot)] ?: slot.defaultAction.name)
        },
    )

    private fun String?.toComponentList(): List<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    suspend fun setDockComponents(components: List<String>) {
        context.dataStore.edit { it[dockKey] = components.joinToString(SEPARATOR) }
    }

    suspend fun setThemedIcons(enabled: Boolean) {
        context.dataStore.edit { it[themedIconsKey] = enabled }
    }

    suspend fun setIconShape(shape: IconShape) {
        context.dataStore.edit { it[iconShapeKey] = shape.name }
    }

    suspend fun setIconPack(packageName: String?) {
        context.dataStore.edit {
            if (packageName == null) it.remove(iconPackKey) else it[iconPackKey] = packageName
        }
    }

    suspend fun setBadgesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[badgesKey] = enabled }
    }

    suspend fun setGesture(slot: GestureSlot, action: GestureAction) {
        context.dataStore.edit { it[gestureKey(slot)] = action.name }
    }

    suspend fun setUpdateUrl(url: String?) {
        context.dataStore.edit {
            if (url.isNullOrBlank()) it.remove(updateUrlKey) else it[updateUrlKey] = url
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = enabled }
    }

    suspend fun setWallpaperDim(percent: Int) {
        context.dataStore.edit { it[wallpaperDimKey] = percent.coerceIn(0, 100) }
    }

    suspend fun setIconSize(dp: Int) {
        context.dataStore.edit { it[iconSizeKey] = dp.coerceIn(36, 72) }
    }

    suspend fun setDrawerLabels(enabled: Boolean) {
        context.dataStore.edit { it[drawerLabelsKey] = enabled }
    }

    suspend fun setSearchAutoFocus(enabled: Boolean) {
        context.dataStore.edit { it[searchAutoFocusKey] = enabled }
    }

    suspend fun setClock24h(enabled: Boolean) {
        context.dataStore.edit { it[clock24hKey] = enabled }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[gridColumnsKey] = columns.coerceIn(3, 6) }
    }

    suspend fun setGridRows(rows: Int) {
        context.dataStore.edit { it[gridRowsKey] = rows.coerceIn(4, 8) }
    }

    suspend fun setHomePageCount(count: Int) {
        context.dataStore.edit { it[homePageCountKey] = count.coerceIn(1, 9) }
    }

    val onboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { it[onboardedKey] ?: false }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardedKey] = complete }
    }

    val homeLayoutResetDone: Flow<Boolean> =
        context.dataStore.data.map { it[homeResetKey] ?: false }

    suspend fun setHomeLayoutResetDone() {
        context.dataStore.edit { it[homeResetKey] = true }
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}

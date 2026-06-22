package com.cunffy.launcher.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

/** Snapshot of all simple launcher settings, derived from DataStore. */
data class LauncherSettings(
    val dockComponents: List<String> = emptyList(),
    val themedIcons: Boolean = false,
    val iconPackPackage: String? = null,
    val badgesEnabled: Boolean = true,
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
    private val iconPackKey = stringPreferencesKey("icon_pack")
    private val badgesKey = booleanPreferencesKey("badges_enabled")
    private fun gestureKey(slot: GestureSlot) = stringPreferencesKey("gesture_${slot.name}")

    val settings: Flow<LauncherSettings> = context.dataStore.data.map { it.toSettings() }

    val dockComponents: Flow<List<String>> =
        context.dataStore.data.map { it[dockKey].toComponentList() }

    private fun Preferences.toSettings() = LauncherSettings(
        dockComponents = this[dockKey].toComponentList(),
        themedIcons = this[themedIconsKey] ?: false,
        iconPackPackage = this[iconPackKey]?.ifBlank { null },
        badgesEnabled = this[badgesKey] ?: true,
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

    private companion object {
        const val SEPARATOR = "|"
    }
}

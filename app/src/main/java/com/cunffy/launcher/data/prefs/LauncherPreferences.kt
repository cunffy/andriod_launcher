package com.cunffy.launcher.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

/**
 * Lightweight persisted launcher state. For the foundation this is just the dock's
 * ordered list of app component strings; home-screen layouts move to Room later.
 */
@Singleton
class LauncherPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dockKey = stringPreferencesKey("dock_components")

    /** Ordered list of flattened [android.content.ComponentName] strings, or empty if unset. */
    val dockComponents: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[dockKey]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setDockComponents(components: List<String>) {
        context.dataStore.edit { it[dockKey] = components.joinToString(SEPARATOR) }
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}

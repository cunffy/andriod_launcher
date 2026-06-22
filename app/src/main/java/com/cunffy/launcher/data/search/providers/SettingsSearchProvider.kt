package com.cunffy.launcher.data.search.providers

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import com.cunffy.launcher.data.search.matchScore
import javax.inject.Inject

/**
 * Surfaces system Settings screens. Android exposes no public settings search index,
 * so we keep a curated table of common destinations, each with keyword aliases and a
 * [Settings] action intent.
 */
class SettingsSearchProvider @Inject constructor() : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        return ENTRIES.mapNotNull { entry ->
            val score = entry.keywords.maxOf { matchScore(it, query) }
            if (score == 0) return@mapNotNull null
            SearchResult(
                id = "setting:${entry.action}",
                title = entry.title,
                subtitle = "Settings",
                type = SearchResultType.SETTING,
                icon = ResultIcon.OfVector(entry.icon),
                score = score,
                onActivate = { context ->
                    context.startActivity(
                        Intent(entry.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        }.sortedByDescending { it.score }
    }

    private data class Entry(
        val title: String,
        val action: String,
        val icon: ImageVector,
        val keywords: List<String>,
    )

    private companion object {
        val ENTRIES = listOf(
            Entry("Wi-Fi", Settings.ACTION_WIFI_SETTINGS, Icons.Rounded.Wifi,
                listOf("wifi", "wi-fi", "wireless", "network", "internet")),
            Entry("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, Icons.Rounded.Bluetooth,
                listOf("bluetooth", "bt", "pair", "headphones")),
            Entry("Display", Settings.ACTION_DISPLAY_SETTINGS, Icons.Rounded.BrightnessMedium,
                listOf("display", "brightness", "screen", "dark mode", "font")),
            Entry("Sound & vibration", Settings.ACTION_SOUND_SETTINGS, Icons.Rounded.VolumeUp,
                listOf("sound", "volume", "ringtone", "vibration", "audio")),
            Entry("Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS, Icons.Rounded.BatteryFull,
                listOf("battery", "power", "saver", "charge")),
            Entry("Apps", Settings.ACTION_APPLICATION_SETTINGS, Icons.Rounded.Apps,
                listOf("apps", "applications", "manage apps")),
            Entry("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, Icons.Rounded.Storage,
                listOf("storage", "space", "memory", "free up")),
            Entry("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS, Icons.Rounded.LocationOn,
                listOf("location", "gps", "maps")),
            Entry("Security", Settings.ACTION_SECURITY_SETTINGS, Icons.Rounded.Lock,
                listOf("security", "lock", "fingerprint", "pin", "password")),
            Entry("Data usage", Settings.ACTION_DATA_USAGE_SETTINGS, Icons.Rounded.DataUsage,
                listOf("data", "usage", "mobile data", "cellular")),
            Entry("All settings", Settings.ACTION_SETTINGS, Icons.Rounded.Settings,
                listOf("settings", "system", "preferences")),
        )
    }
}

package com.cunffy.launcher.data.search.providers

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import com.cunffy.launcher.data.search.matchScore
import com.cunffy.launcher.ui.settings.SettingsActivity
import javax.inject.Inject

/** "Command palette": run launcher actions straight from search. */
class CommandSearchProvider @Inject constructor() : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        return COMMANDS.mapNotNull { command ->
            val score = command.keywords.maxOf { matchScore(it, query) }
            if (score == 0) return@mapNotNull null
            SearchResult(
                id = "cmd:${command.title}",
                title = command.title,
                subtitle = "Command",
                type = SearchResultType.COMMAND,
                icon = ResultIcon.OfVector(command.icon),
                score = score,
                onActivate = command.action,
            )
        }.sortedByDescending { it.score }
    }

    private data class Command(
        val title: String,
        val icon: ImageVector,
        val keywords: List<String>,
        val action: (Context) -> Unit,
    )

    private companion object {
        private fun openSettings(context: Context, destination: String) {
            context.startActivity(
                Intent(context, SettingsActivity::class.java)
                    .putExtra(SettingsActivity.EXTRA_DESTINATION, destination)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        val COMMANDS = listOf(
            Command("Launcher settings", Icons.Rounded.Settings,
                listOf("launcher settings", "settings", "preferences")) {
                openSettings(it, SettingsActivity.DEST_ROOT)
            },
            Command("Appearance & icons", Icons.Rounded.Palette,
                listOf("appearance", "icons", "icon pack", "theme", "themed")) {
                openSettings(it, SettingsActivity.DEST_APPEARANCE)
            },
            Command("Edit gestures", Icons.Rounded.Gesture,
                listOf("gestures", "swipe", "gesture")) {
                openSettings(it, SettingsActivity.DEST_GESTURES)
            },
            Command("Hidden apps", Icons.Rounded.VisibilityOff,
                listOf("hidden apps", "hide", "hidden")) {
                openSettings(it, SettingsActivity.DEST_HIDDEN)
            },
            Command("Change wallpaper", Icons.Rounded.Wallpaper,
                listOf("wallpaper", "background")) { context ->
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SET_WALLPAPER), "Set wallpaper",
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            Command("Back up & restore", Icons.Rounded.Bolt,
                listOf("backup", "restore", "export", "import")) {
                openSettings(it, SettingsActivity.DEST_BACKUP)
            },
        )
    }
}

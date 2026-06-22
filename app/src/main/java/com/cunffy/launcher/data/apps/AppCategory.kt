package com.cunffy.launcher.data.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Smart Launcher-style app groupings. Each maps to a Material icon for the drawer
 * category sidebar. [ALL] is a synthetic bucket shown first.
 */
enum class AppCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Rounded.Apps),
    COMMUNICATION("Social", Icons.Rounded.Groups),
    MUSIC("Music & Audio", Icons.Rounded.MusicNote),
    VIDEO("Video", Icons.Rounded.PlayArrow),
    GAMES("Games", Icons.Rounded.SportsEsports),
    NEWS("News", Icons.AutoMirrored.Rounded.Article),
    MAPS("Maps & Travel", Icons.Rounded.Map),
    PRODUCTIVITY("Productivity", Icons.Rounded.WorkOutline),
    CREATIVE("Photo & Creative", Icons.Rounded.Brush),
    OTHER("Other", Icons.Rounded.Apps),
    ;

    companion object {
        /** Categories that should appear in the sidebar (ALL plus any non-empty group). */
        val sidebarOrder: List<AppCategory> = entries
    }
}

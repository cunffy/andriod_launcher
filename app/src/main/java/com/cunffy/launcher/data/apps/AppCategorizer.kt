package com.cunffy.launcher.data.apps

import android.content.pm.ApplicationInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assigns an [AppCategory] to an app. Primary signal is the OS-declared
 * [ApplicationInfo.category]; for apps that declare none we fall back to a small
 * heuristic table keyed on well-known package names/prefixes.
 */
@Singleton
class AppCategorizer @Inject constructor() {

    fun categorize(appInfo: ApplicationInfo): AppCategory {
        fromSystemCategory(appInfo.category)?.let { return it }
        return fromPackageHeuristic(appInfo.packageName)
    }

    private fun fromSystemCategory(category: Int): AppCategory? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.CREATIVE
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.COMMUNICATION
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        else -> null
    }

    private fun fromPackageHeuristic(packageName: String): AppCategory {
        val pkg = packageName.lowercase()
        return HEURISTICS.firstOrNull { (needles, _) ->
            needles.any { pkg.contains(it) }
        }?.second ?: AppCategory.OTHER
    }

    private companion object {
        // Ordered list of (substrings -> category). First match wins.
        val HEURISTICS: List<Pair<List<String>, AppCategory>> = listOf(
            listOf("whatsapp", "telegram", "messenger", "signal", "discord", "slack",
                "gmail", "email", "mail", "contacts", "dialer", "phone", "messaging",
                "twitter", "instagram", "facebook", "snapchat", "tiktok", "reddit",
                "linkedin", "mastodon", "threads") to AppCategory.COMMUNICATION,
            listOf("spotify", "music", "audio", "podcast", "soundcloud", "deezer",
                "tidal", "pandora") to AppCategory.MUSIC,
            listOf("youtube", "netflix", "video", "hulu", "disney", "twitch", "plex",
                "vlc", "primevideo") to AppCategory.VIDEO,
            listOf("game", "minecraft", "roblox", "genshin") to AppCategory.GAMES,
            listOf("news", "feedly", "flipboard") to AppCategory.NEWS,
            listOf("maps", "waze", "uber", "lyft", "navigation", "transit", "flight",
                "booking", "airbnb") to AppCategory.MAPS,
            listOf("docs", "sheets", "office", "word", "excel", "notion", "keep",
                "calendar", "drive", "dropbox", "todo", "tasks", "bank", "wallet",
                "pay") to AppCategory.PRODUCTIVITY,
            listOf("camera", "photo", "gallery", "lightroom", "snapseed", "figma",
                "canva", "draw", "paint") to AppCategory.CREATIVE,
        )
    }
}

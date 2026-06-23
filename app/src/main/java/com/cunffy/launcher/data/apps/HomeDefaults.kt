package com.cunffy.launcher.data.apps

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The apps seeded onto the home screen's bottom two rows. The upper row holds favorite apps
 * (Chrome, Crunchyroll, YouTube, Genshin Impact) and the lower row holds the everyday
 * essentials (Camera, Photos, Files). Each is resolved to whatever is installed (by package,
 * then by app name); missing favorites are skipped and the essentials row is topped up with
 * other installed apps so it never seeds empty. All entries are de-duplicated across both rows.
 */
@Singleton
class HomeDefaults @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Returns [favoritesRow, essentialsRow] of component keys, bottom-anchored by the caller. */
    fun mainPageRows(apps: List<AppInfo>): List<List<String>> {
        val used = LinkedHashSet<String>()

        fun byPackage(vararg pkgs: String): String? {
            for (pkg in pkgs) {
                apps.firstOrNull { it.packageName == pkg }?.let { return it.componentKey }
            }
            return null
        }
        fun byLabel(vararg keywords: String): String? =
            apps.firstOrNull { app ->
                val label = app.label.lowercase()
                keywords.any { label.contains(it) }
            }?.componentKey
        fun resolve(intent: Intent): String? = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        }.getOrNull()?.let { if (it == "android") null else byPackage(it) }

        val favorites = mutableListOf<String>()
        fun addFavorite(key: String?) {
            if (key != null && used.add(key)) favorites.add(key)
        }
        addFavorite(byPackage("com.android.chrome") ?: byLabel("chrome"))
        addFavorite(byPackage("com.crunchyroll.crunchyroid") ?: byLabel("crunchyroll"))
        addFavorite(byPackage("com.google.android.youtube") ?: byLabel("youtube"))
        addFavorite(
            byPackage(
                "com.miHoYo.GenshinImpact",
                "com.miHoYo.GenshinImpactOversea",
                "com.miHoYo.ys",
            ) ?: byLabel("genshin"),
        )

        val essentials = mutableListOf<String>()
        fun addEssential(key: String?) {
            if (key != null && essentials.size < ROW_SIZE && used.add(key)) essentials.add(key)
        }
        addEssential(resolve(Intent("android.media.action.STILL_IMAGE_CAMERA")) ?: byLabel("camera"))
        addEssential(byPackage("com.google.android.apps.photos") ?: byLabel("photos", "gallery"))
        addEssential(
            byPackage("com.google.android.documentsui", "com.android.documentsui")
                ?: byLabel("files", "file manager"),
        )
        addEssential(byPackage("com.android.vending") ?: byLabel("play store"))
        // If any of the above weren't installed, top up from a curated list of common apps
        // (never arbitrary alphabetical picks like an authenticator app).
        val curatedFallback = listOf(
            byPackage("com.google.android.gm") ?: byLabel("gmail"),
            byPackage("com.google.android.apps.maps") ?: byLabel("maps"),
            byPackage("com.google.android.calendar") ?: byLabel("calendar"),
            byPackage("com.google.android.deskclock", "com.android.deskclock") ?: byLabel("clock"),
            byPackage("com.android.settings") ?: byLabel("settings"),
        )
        for (key in curatedFallback) {
            if (essentials.size >= ROW_SIZE) break
            addEssential(key)
        }

        return listOf(favorites, essentials)
    }

    private companion object {
        /** Apps per grid row. */
        const val ROW_SIZE = 4
    }
}

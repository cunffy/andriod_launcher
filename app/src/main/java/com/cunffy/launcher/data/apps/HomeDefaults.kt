package com.cunffy.launcher.data.apps

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The everyday apps seeded onto the bottom row of the home screen: Camera, Photos, Files, and
 * a browser — resolved to whatever is installed (by package, then by app name). Missing ones
 * are skipped.
 */
@Singleton
class HomeDefaults @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun bottomRowKeys(apps: List<AppInfo>): List<String> {
        val result = LinkedHashSet<String>()

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
        fun add(key: String?) { if (key != null) result.add(key) }

        add(resolve(Intent("android.media.action.STILL_IMAGE_CAMERA")) ?: byLabel("camera"))
        add(byPackage("com.google.android.apps.photos") ?: byLabel("photos", "gallery"))
        add(byPackage("com.google.android.documentsui", "com.android.documentsui")
            ?: byLabel("files", "file manager"))
        add(byPackage("com.android.chrome") ?: byLabel("chrome", "browser"))

        // Guarantee a full bottom row even if some defaults aren't installed: top up with the
        // first available apps so the home screen never seeds empty.
        if (result.size < ROW_SIZE) {
            for (app in apps) {
                if (result.size >= ROW_SIZE) break
                result.add(app.componentKey)
            }
        }

        return result.take(ROW_SIZE)
    }

    private companion object {
        /** Number of shortcuts seeded along the bottom row (one grid row). */
        const val ROW_SIZE = 4
    }
}

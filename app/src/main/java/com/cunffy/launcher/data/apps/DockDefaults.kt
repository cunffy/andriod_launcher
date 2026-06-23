package com.cunffy.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes the default dock: the apps the user asked to keep handy — Phone, Messages,
 * Spotify, simPRO, Slack — resolved to whatever is installed (by package, then by app name).
 * Anything not installed is simply skipped; the dock is never padded with random apps.
 */
@Singleton
class DockDefaults @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun defaultKeys(apps: List<AppInfo>): List<String> {
        val result = LinkedHashSet<String>()

        fun byPackage(pkg: String?): String? =
            if (pkg.isNullOrBlank() || pkg == "android") null
            else apps.firstOrNull { it.packageName == pkg }?.componentKey

        fun byLabel(vararg keywords: String): String? =
            apps.firstOrNull { app ->
                val label = app.label.lowercase()
                keywords.any { label.contains(it) }
            }?.componentKey

        fun add(key: String?) { if (key != null) result.add(key) }

        // Phone / dialer (resolve the default dialer, else known packages, else by name).
        add(byPackage(resolvePackage(Intent(Intent.ACTION_DIAL)))
            ?: byPackage("com.google.android.dialer")
            ?: byPackage("com.android.dialer")
            ?: byLabel("phone", "dialer"))
        // Messages (default SMS app, else by name).
        add(byPackage(Telephony.Sms.getDefaultSmsPackage(context)) ?: byLabel("messages", "messaging"))
        add(byPackage("com.spotify.music") ?: byLabel("spotify"))
        add(byPackage("com.simprogroup.simpromobile") ?: byLabel("simpro", "simpro mobile"))
        add(byPackage("com.Slack") ?: byLabel("slack"))

        return result.toList()
    }

    private fun resolvePackage(intent: Intent): String? = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }.getOrNull()
}

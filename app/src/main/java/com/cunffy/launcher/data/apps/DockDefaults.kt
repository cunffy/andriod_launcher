package com.cunffy.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes the default dock: the apps the user asked to keep handy — Contacts, Messages,
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

        // Contacts (resolve the system contacts app, else match by name).
        add(byPackage(resolvePackage(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)))
            ?: byLabel("contacts"))
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

package com.cunffy.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes a sensible default dock when the user hasn't customized one: the apps people
 * actually keep handy, resolved to whatever is installed. Order: contacts, messages, Spotify,
 * simPRO, Slack, then camera, photos and files — padded with other apps to fill the row.
 */
@Singleton
class DockDefaults @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun defaultKeys(apps: List<AppInfo>, size: Int = 8): List<String> {
        val byPackage = apps.groupBy { it.packageName }
        val ordered = LinkedHashSet<String>()

        fun addPackage(pkg: String?) {
            if (pkg.isNullOrBlank() || pkg == "android") return
            byPackage[pkg]?.firstOrNull()?.let { ordered.add(it.componentKey) }
        }
        fun addIntent(intent: Intent) = addPackage(resolvePackage(intent))

        // Curated, in priority order.
        addIntent(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
        addPackage(Telephony.Sms.getDefaultSmsPackage(context))
        addPackage("com.spotify.music")
        addPackage("com.simprogroup.simpromobile")
        addPackage("com.Slack")
        addIntent(Intent("android.media.action.STILL_IMAGE_CAMERA"))
        addPackage("com.google.android.apps.photos")
        addIntent(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.EMPTY, "*/*"))
        addPackage("com.google.android.documentsui")
        addPackage("com.android.documentsui")

        // Pad to [size] with any remaining apps so the dock is never short.
        for (app in apps) {
            if (ordered.size >= size) break
            ordered.add(app.componentKey)
        }
        return ordered.take(size).toList()
    }

    private fun resolvePackage(intent: Intent): String? = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }.getOrNull()
}

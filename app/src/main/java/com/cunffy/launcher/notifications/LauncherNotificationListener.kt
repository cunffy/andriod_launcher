package com.cunffy.launcher.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Mirrors active notifications into [NotificationBadgeStore] so the launcher can draw a dot
 * on app icons. Requires the user to grant notification access in system settings; the
 * system binds/unbinds this service accordingly.
 *
 * Counts are maintained incrementally from each callback. The previous implementation
 * re-fetched every active notification from system_server on every event — a heavy binder
 * call that media apps triggered about once per second (progress-bar notification updates),
 * screen on or off, which was a major battery drain. Now a media/progress update (not
 * clearable, count unchanged) costs two map operations and publishes nothing.
 */
@AndroidEntryPoint
class LauncherNotificationListener : NotificationListenerService() {

    @Inject lateinit var badgeStore: NotificationBadgeStore

    /** Clearable notifications we know about: notification key → package name. */
    private val clearableKeys = HashMap<String, String>()

    override fun onListenerConnected() {
        // The one full fetch, to seed state at bind time.
        clearableKeys.clear()
        runCatching { activeNotifications }.getOrNull()?.forEach { sbn ->
            if (sbn.isClearable) clearableKeys[sbn.key] = sbn.packageName
        }
        publish()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // A posted event can also be an update that flips clearability; track both directions.
        val changed = if (sbn.isClearable) {
            clearableKeys.put(sbn.key, sbn.packageName) == null
        } else {
            clearableKeys.remove(sbn.key) != null
        }
        if (changed) publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        if (clearableKeys.remove(sbn.key) != null) publish()
    }

    private fun publish() {
        badgeStore.update(clearableKeys.values.groupingBy { it }.eachCount())
    }
}

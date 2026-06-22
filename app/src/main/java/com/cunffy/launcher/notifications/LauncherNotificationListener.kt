package com.cunffy.launcher.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Mirrors active notifications into [NotificationBadgeStore] so the launcher can draw a dot
 * on app icons. Requires the user to grant notification access in system settings; the
 * system binds/unbinds this service accordingly.
 */
@AndroidEntryPoint
class LauncherNotificationListener : NotificationListenerService() {

    @Inject lateinit var badgeStore: NotificationBadgeStore

    override fun onListenerConnected() {
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val counts = active
            .filter { it.isClearable }
            .groupingBy { it.packageName }
            .eachCount()
        badgeStore.update(counts)
    }
}

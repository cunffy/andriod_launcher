package com.cunffy.launcher.gesture

import android.annotation.SuppressLint
import android.content.Context

/**
 * Expands the system notification shade — used by the swipe-down-on-home gesture.
 *
 * There is no public API for this, so we call the hidden StatusBarManager#expandNotificationsPanel
 * via reflection (paired with the EXPAND_STATUS_BAR permission). This is best-effort and
 * may stop working on future Android versions; the planned long-term path is an
 * AccessibilityService action (GLOBAL_ACTION_NOTIFICATIONS).
 */
object NotificationShade {

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expand(context: Context): Boolean = invoke(context, "expandNotificationsPanel")

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandQuickSettings(context: Context): Boolean = invoke(context, "expandSettingsPanel")

    @SuppressLint("PrivateApi")
    private fun invoke(context: Context, method: String): Boolean = runCatching {
        val service = context.getSystemService("statusbar") ?: return false
        Class.forName("android.app.StatusBarManager")
            .getMethod(method)
            .invoke(service)
        true
    }.getOrDefault(false)
}

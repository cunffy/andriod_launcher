package com.cunffy.launcher.gesture

import android.annotation.SuppressLint
import android.content.Context
import com.cunffy.launcher.accessibility.LauncherAccessibilityService

/**
 * Expands the system notification shade / quick settings — used by the home swipe-down gesture.
 *
 * Prefers the supported [LauncherAccessibilityService] global action when the user has enabled
 * it; otherwise falls back to the hidden StatusBarManager methods via reflection (paired with
 * the EXPAND_STATUS_BAR permission), which is best-effort.
 */
object NotificationShade {

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expand(context: Context): Boolean {
        LauncherAccessibilityService.instance?.let { return it.expandNotifications() }
        return invoke(context, "expandNotificationsPanel")
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandQuickSettings(context: Context): Boolean {
        LauncherAccessibilityService.instance?.let { return it.expandQuickSettings() }
        return invoke(context, "expandSettingsPanel")
    }

    @SuppressLint("PrivateApi")
    private fun invoke(context: Context, method: String): Boolean = runCatching {
        val service = context.getSystemService("statusbar") ?: return false
        Class.forName("android.app.StatusBarManager")
            .getMethod(method)
            .invoke(service)
        true
    }.getOrDefault(false)
}

package com.cunffy.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional accessibility service that lets the launcher open the notification shade and quick
 * settings reliably (the public, supported path — the [com.cunffy.launcher.gesture.NotificationShade]
 * reflection fallback is used until the user enables this in Accessibility settings).
 */
class LauncherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun expandNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    fun expandQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    companion object {
        @Volatile
        var instance: LauncherAccessibilityService? = null
    }
}

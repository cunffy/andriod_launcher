package com.cunffy.launcher.gesture

import android.content.Context
import android.content.Intent
import com.cunffy.launcher.accessibility.LauncherAccessibilityService
import com.cunffy.launcher.ui.settings.SettingsActivity

/**
 * Locks the screen via the supported [LauncherAccessibilityService] global action. When the
 * service isn't enabled there's no app-level way to lock the device, so we send the user to the
 * launcher's settings (where the accessibility toggle lives) and report failure.
 */
object ScreenLock {
    fun lock(context: Context): Boolean {
        LauncherAccessibilityService.instance?.let { return it.lockScreen() }
        runCatching {
            context.startActivity(
                Intent(context, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return false
    }
}

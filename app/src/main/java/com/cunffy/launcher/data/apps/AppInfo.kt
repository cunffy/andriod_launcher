package com.cunffy.launcher.data.apps

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * A single launchable app entry, resolved from [android.content.pm.LauncherApps].
 * [key] uniquely identifies the activity+user so the same app on different profiles
 * (e.g. work profile) does not collide.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val category: AppCategory,
    val icon: Drawable,
    val hidden: Boolean = false,
    val locked: Boolean = false,
) {
    val key: String = "${componentName.flattenToString()}#${user.hashCode()}"

    /** Stable identity used for persistence (ignores user, keyed on the component). */
    val componentKey: String = componentName.flattenToString()
}

package com.cunffy.launcher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the live list of launchable apps. Backed by [LauncherApps], which handles
 * multiple user profiles and emits install/update/remove callbacks we listen to so
 * the drawer stays current without manual refresh.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categorizer: AppCategorizer,
) {
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload()
        override fun onPackagesAvailable(
            packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
        ) = reload()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
        ) = reload()
    }

    init {
        launcherApps.registerCallback(callback)
        reload()
    }

    private fun reload() {
        scope.launch { _apps.value = loadAll() }
    }

    private fun loadAll(): List<AppInfo> {
        val ownPackage = context.packageName
        return userManager.userProfiles.flatMap { user ->
            launcherApps.getActivityList(null, user)
                // Hide the launcher itself from its own drawer (Pixel does the same).
                .filter { it.applicationInfo.packageName != ownPackage }
                .map { activity ->
                    AppInfo(
                        label = activity.label.toString(),
                        packageName = activity.applicationInfo.packageName,
                        componentName = activity.componentName,
                        user = user,
                        category = categorizer.categorize(activity.applicationInfo),
                        icon = activity.getBadgedIcon(0),
                    )
                }
        }.sortedBy { it.label.lowercase() }
    }

    /** Launch an app, anchoring its open animation to [sourceBounds] when provided. */
    fun launch(app: AppInfo, sourceBounds: Rect? = null) {
        launcherApps.startMainActivity(app.componentName, app.user, sourceBounds, null)
    }

    fun launch(componentName: ComponentName, user: UserHandle = Process.myUserHandle()) {
        launcherApps.startMainActivity(componentName, user, null, null)
    }
}

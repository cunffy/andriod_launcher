package com.cunffy.launcher.data.icons

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Discovers installed icon packs and caches loaded [IconPack] parsers. */
@Singleton
class IconPackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = HashMap<String, IconPack>()

    /** Intent actions that icon-pack apps register so launchers can find them. */
    private val packActions = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME",
    )

    fun installedPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, IconPackInfo>()
        packActions.forEach { action ->
            val intent = Intent(action)
            pm.queryIntentActivities(intent, 0).forEach { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg !in seen) {
                    seen[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString())
                }
            }
        }
        return seen.values.toList()
    }

    fun pack(packageName: String): IconPack =
        cache.getOrPut(packageName) { IconPack(context, packageName) }

    fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}

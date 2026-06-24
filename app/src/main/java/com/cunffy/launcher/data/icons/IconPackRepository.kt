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
        "com.novalauncher.THEME",
        "org.adw.launcher.THEMES",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
        "com.gau.go.launcherex.theme",
        "com.teslacoilsw.launcher.THEME",
    )

    /** Some packs (Apex-style) only advertise via a MAIN-intent category instead of an action. */
    private val packCategories = listOf(
        "com.anddoes.launcher.THEME",
    )

    fun installedPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, IconPackInfo>()
        fun add(ri: android.content.pm.ResolveInfo) {
            val pkg = ri.activityInfo.packageName
            if (pkg != null && pkg !in seen) {
                seen[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString())
            }
        }
        packActions.forEach { action ->
            runCatching { pm.queryIntentActivities(Intent(action), 0) }
                .getOrDefault(emptyList())
                .forEach { add(it) }
        }
        packCategories.forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            runCatching { pm.queryIntentActivities(intent, 0) }
                .getOrDefault(emptyList())
                .forEach { add(it) }
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

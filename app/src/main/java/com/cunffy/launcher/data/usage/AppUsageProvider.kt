package com.cunffy.launcher.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Per-app usage figures used by the drawer's "recent" and "most used" sort modes. */
data class AppUsage(
    val lastUsed: Long,
    val totalTimeMs: Long,
)

/**
 * Reads app usage from [UsageStatsManager]. This needs the special "usage access" permission,
 * which the user grants in system settings (it can't be requested with a runtime dialog), so
 * every call is guarded by [hasPermission] and falls back to empty data when not granted.
 */
@Singleton
class AppUsageProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = runCatching {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }.getOrDefault(AppOpsManager.MODE_DEFAULT)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Intent that opens the system "Usage access" screen so the user can grant permission. */
    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Map of package name → usage over the last ~30 days; empty when permission is missing. */
    fun usageByPackage(): Map<String, AppUsage> {
        if (!hasPermission()) return emptyMap()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - THIRTY_DAYS_MS
        val stats = runCatching {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
        }.getOrNull().orEmpty()
        // A package can appear in several buckets; keep the latest use and sum foreground time.
        val merged = HashMap<String, AppUsage>()
        stats.forEach { s ->
            val existing = merged[s.packageName]
            merged[s.packageName] = AppUsage(
                lastUsed = maxOf(existing?.lastUsed ?: 0L, s.lastTimeUsed),
                totalTimeMs = (existing?.totalTimeMs ?: 0L) + s.totalTimeInForeground,
            )
        }
        return merged
    }

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }
}

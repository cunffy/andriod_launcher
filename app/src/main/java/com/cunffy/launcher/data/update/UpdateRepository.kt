package com.cunffy.launcher.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.cunffy.launcher.BuildConfig
import com.cunffy.launcher.core.Http
import com.cunffy.launcher.data.prefs.LauncherPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Describes the latest available build, hosted as a small JSON manifest (e.g. on a GitHub
 * release). Point the update URL at it from Settings, then the launcher can self-update by
 * downloading [apkUrl] and handing it to the system package installer.
 */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String = "",
    val apkUrl: String,
    val notes: String = "",
)

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: LauncherPreferences,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns the manifest only if it advertises a newer versionCode than the installed app. */
    suspend fun checkForUpdate(): UpdateManifest? {
        val url = resolveUrl() ?: return null
        val body = Http.getString(url) ?: return null
        val manifest = runCatching { json.decodeFromString(UpdateManifest.serializer(), body) }
            .getOrNull() ?: return null
        return manifest.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    }

    suspend fun downloadApk(manifest: UpdateManifest): File? {
        cleanupDownloads()
        val target = File(context.cacheDir, "updates/update-${manifest.versionCode}.apk")
        return if (Http.download(manifest.apkUrl, target)) target else null
    }

    /**
     * Deletes leftover update APKs (~45 MB each — before this cleanup existed, one per ever-
     * installed release accumulated in cache) and abandons any stale installer sessions of
     * ours, which each stage another copy of the APK.
     */
    fun cleanupDownloads() {
        runCatching {
            File(context.cacheDir, "updates").listFiles()?.forEach { it.delete() }
        }
        runCatching {
            val installer = context.packageManager.packageInstaller
            installer.mySessions.forEach { info ->
                runCatching { installer.abandonSession(info.sessionId) }
            }
        }
    }

    /**
     * Installs a downloaded APK via [PackageInstaller]. Because this is the launcher updating
     * *itself* with the same signing key, on Android 12+ we request USER_ACTION_NOT_REQUIRED so
     * the update applies seamlessly — no "scan / confirm" prompt each time. If the system still
     * requires confirmation, [InstallResultReceiver] surfaces the one-tap dialog as a fallback.
     */
    fun installApk(file: File) {
        // Silent installs are impossible until the user lets this app install unknown apps;
        // without it every update shows the system installer. Send them to grant it, then return
        // so they can re-run the update.
        if (!canInstallPackages()) {
            requestInstallPermission()
            return
        }
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(
                    PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED,
                )
            }
            // Android 14+ only allows silent self-updates by the app that *owns* updates. Claim
            // ownership so that, once granted, future updates install with no prompt.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching { params.setRequestUpdateOwnership(true) }
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("update", 0, file.length()).use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                    session.fsync(out)
                }
                val callback = Intent(InstallResultReceiver.ACTION)
                    .setPackage(context.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }
                val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
                session.commit(pending.intentSender)
            }
        }
        // The session holds its own copy of the bytes once committed; the download is dead weight.
        runCatching { file.delete() }
    }

    /** Send the user to the "install unknown apps" screen for this launcher. */
    fun requestInstallPermission() {
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    private suspend fun resolveUrl(): String? {
        preferences.settings.first().updateUrl?.let { return it }
        return BuildConfig.UPDATE_MANIFEST_URL.ifBlank { null }
    }
}

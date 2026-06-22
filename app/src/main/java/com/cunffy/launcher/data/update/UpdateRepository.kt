package com.cunffy.launcher.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
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
        val target = File(context.cacheDir, "updates/update-${manifest.versionCode}.apk")
        return if (Http.download(manifest.apkUrl, target)) target else null
    }

    /** Launches the system installer for a downloaded APK. */
    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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

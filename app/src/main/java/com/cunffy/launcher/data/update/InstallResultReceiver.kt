package com.cunffy.launcher.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

/**
 * Receives the outcome of a [PackageInstaller] self-update. A seamless update (same signing key,
 * Android 12+) completes without any prompt; if the system still insists on confirmation it
 * sends [PackageInstaller.STATUS_PENDING_USER_ACTION], and we launch the one-tap confirm dialog
 * as a fallback.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
            confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(confirm) }
        }
    }

    companion object {
        const val ACTION = "com.cunffy.launcher.INSTALL_RESULT"
    }
}

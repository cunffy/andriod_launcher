package com.cunffy.launcher.data.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import com.cunffy.launcher.notifications.LauncherNotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads active media sessions (e.g. Spotify) so the launcher can show a now-playing card with
 * transport controls. Requires notification access (the same grant used for badges); without
 * it [activeController] simply returns null.
 */
@Singleton
class MediaSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent =
        ComponentName(context, LauncherNotificationListener::class.java)

    fun activeController(): MediaController? = runCatching {
        sessionManager.getActiveSessions(listenerComponent).firstOrNull()
    }.getOrNull()
}

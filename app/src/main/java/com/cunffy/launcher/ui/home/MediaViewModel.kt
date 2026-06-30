package com.cunffy.launcher.ui.home

import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.lifecycle.ViewModel
import com.cunffy.launcher.data.media.MediaSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSessionRepository: MediaSessionRepository,
) : ViewModel() {

    data class NowPlaying(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val albumArt: android.graphics.Bitmap?,
    )

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying = _nowPlaying.asStateFlow()

    private var controller: MediaController? = null
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() {
            controller = null
            _nowPlaying.value = null
        }
    }

    // Fires when media starts/stops/switches apps, so we rebind without polling.
    private val sessionsListener =
        android.media.session.MediaSessionManager.OnActiveSessionsChangedListener { rebind() }
    private var observing = false

    /** Begin observing media sessions (call when the home screen becomes visible). Idempotent. */
    fun start() {
        if (observing) return
        observing = true
        mediaSessionRepository.registerSessionsChanged(sessionsListener)
        rebind()
    }

    /** Stop observing (call when the home screen is backgrounded) so nothing runs in the dark. */
    fun stop() {
        if (!observing) return
        observing = false
        mediaSessionRepository.unregisterSessionsChanged(sessionsListener)
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun rebind() {
        controller?.unregisterCallback(callback)
        controller = mediaSessionRepository.activeController()
        controller?.registerCallback(callback)
        publish()
    }

    private fun publish() {
        val c = controller
        val metadata = c?.metadata
        if (c == null || metadata == null) {
            _nowPlaying.value = null
            return
        }
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        if (title.isBlank() && artist.isBlank()) {
            _nowPlaying.value = null
            return
        }
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        _nowPlaying.value = NowPlaying(
            title = title,
            artist = artist,
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            albumArt = art,
        )
    }

    fun togglePlayPause() {
        val controls = controller?.transportControls ?: return
        if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controls.pause()
        } else {
            controls.play()
        }
    }

    fun next() = controller?.transportControls?.skipToNext()

    fun previous() = controller?.transportControls?.skipToPrevious()

    /** Open the app that owns the current session. */
    fun openApp() {
        val c = controller ?: return
        // Launch the owning app directly — a normal foreground activity start that reliably
        // brings it up. (The session's own PendingIntent can be silently dropped by background-
        // activity-launch rules, so we only use it as a fallback.)
        val pkg = c.packageName
        if (pkg != null) {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                if (runCatching {
                        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        true
                    }.getOrDefault(false)
                ) {
                    return
                }
            }
        }
        c.sessionActivity?.let { runCatching { it.send() } }
    }

    override fun onCleared() {
        stop()
    }
}

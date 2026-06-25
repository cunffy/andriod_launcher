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

    /** Re-binds to the current active session (call when the home screen resumes). */
    fun refresh() {
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

    /** Open the app that owns the current session — its own now-playing UI if it offers one. */
    fun openApp() {
        val c = controller ?: return
        // Prefer the session's declared activity (drops you on its player screen)…
        c.sessionActivity?.let {
            runCatching { it.send() }.onSuccess { return }
        }
        // …otherwise just launch the owning app.
        val pkg = c.packageName ?: return
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        runCatching {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onCleared() {
        controller?.unregisterCallback(callback)
    }
}

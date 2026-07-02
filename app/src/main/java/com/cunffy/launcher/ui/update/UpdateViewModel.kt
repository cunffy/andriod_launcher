package com.cunffy.launcher.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.update.UpdateManifest
import com.cunffy.launcher.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val preferences: LauncherPreferences,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data class Available(val manifest: UpdateManifest) : State
        data object Downloading : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    private var checked = false

    /**
     * Checks for a newer build at most once every 12 hours (not on every cold start, which on
     * aggressive ROMs can be very often), and surfaces the update prompt if one is found.
     */
    fun checkOnLaunch() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            // Reclaim leftover update APKs from cache every launch (cheap; a directory listing
            // when there's nothing to delete), regardless of the network-check throttle below.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                updateRepository.cleanupDownloads()
            }
            val now = System.currentTimeMillis()
            val last = preferences.lastUpdateCheck.first()
            if (now - last < CHECK_INTERVAL_MS) return@launch
            preferences.setLastUpdateCheck(now)
            updateRepository.checkForUpdate()?.let { _state.value = State.Available(it) }
        }
    }

    fun startUpdate(manifest: UpdateManifest) {
        viewModelScope.launch {
            _state.value = State.Downloading
            val apk = updateRepository.downloadApk(manifest)
            if (apk != null) {
                updateRepository.installApk(apk)
                _state.value = State.Idle
            } else {
                _state.value = State.Failed("Download failed")
            }
        }
    }

    fun dismiss() { _state.value = State.Idle }

    private companion object {
        const val CHECK_INTERVAL_MS = 12L * 60 * 60 * 1000
    }
}

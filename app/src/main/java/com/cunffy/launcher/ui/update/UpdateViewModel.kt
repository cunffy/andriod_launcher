package com.cunffy.launcher.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.update.UpdateManifest
import com.cunffy.launcher.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
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

    /** Checks once per process for a newer build and surfaces the update prompt if found. */
    fun checkOnLaunch() {
        if (checked) return
        checked = true
        viewModelScope.launch {
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
}

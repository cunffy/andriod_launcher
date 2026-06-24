package com.cunffy.launcher.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.apps.AppCatalog
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.backup.BackupManager
import com.cunffy.launcher.data.customization.CustomizationRepository
import com.cunffy.launcher.data.icons.IconPackInfo
import com.cunffy.launcher.data.icons.IconPackRepository
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.data.prefs.LauncherSettings
import com.cunffy.launcher.data.update.UpdateRepository
import com.cunffy.launcher.gesture.GestureAction
import com.cunffy.launcher.gesture.GestureSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: LauncherPreferences,
    private val iconPackRepository: IconPackRepository,
    private val customizationRepository: CustomizationRepository,
    private val backupManager: BackupManager,
    private val updateRepository: UpdateRepository,
    appCatalog: AppCatalog,
) : ViewModel() {

    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherSettings())

    val allApps = appCatalog.allApps

    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val iconPacks = _iconPacks.asStateFlow()

    init {
        viewModelScope.launch {
            _iconPacks.value = withContext(Dispatchers.IO) { iconPackRepository.installedPacks() }
        }
    }

    fun setThemedIcons(enabled: Boolean) = viewModelScope.launch {
        preferences.setThemedIcons(enabled)
    }

    fun setIconShape(shape: com.cunffy.launcher.data.prefs.IconShape) = viewModelScope.launch {
        preferences.setIconShape(shape)
    }

    fun setThemeMode(mode: com.cunffy.launcher.data.prefs.ThemeMode) = viewModelScope.launch {
        preferences.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        preferences.setDynamicColor(enabled)
    }

    fun setAccentPreset(preset: com.cunffy.launcher.data.prefs.AccentPreset) = viewModelScope.launch {
        preferences.setAccentPreset(preset)
    }

    fun setShowAtAGlance(show: Boolean) = viewModelScope.launch {
        preferences.setShowAtAGlance(show)
    }

    fun setShowMediaCard(show: Boolean) = viewModelScope.launch {
        preferences.setShowMediaCard(show)
    }

    fun setClockSize(sp: Int) = viewModelScope.launch { preferences.setClockSize(sp) }

    fun setDrawerColumns(columns: Int) = viewModelScope.launch {
        preferences.setDrawerColumns(columns)
    }

    fun setDrawerOpacity(percent: Int) = viewModelScope.launch {
        preferences.setDrawerOpacity(percent)
    }

    fun setWallpaperDim(percent: Int) = viewModelScope.launch {
        preferences.setWallpaperDim(percent)
    }

    fun setIconSize(dp: Int) = viewModelScope.launch { preferences.setIconSize(dp) }

    fun setDrawerLabels(enabled: Boolean) = viewModelScope.launch {
        preferences.setDrawerLabels(enabled)
    }

    fun setHomeLabels(enabled: Boolean) = viewModelScope.launch {
        preferences.setHomeLabels(enabled)
    }

    fun setSearchAutoFocus(enabled: Boolean) = viewModelScope.launch {
        preferences.setSearchAutoFocus(enabled)
    }

    fun setClock24h(enabled: Boolean) = viewModelScope.launch { preferences.setClock24h(enabled) }

    fun setGridColumns(columns: Int) = viewModelScope.launch { preferences.setGridColumns(columns) }

    fun setGridRows(rows: Int) = viewModelScope.launch { preferences.setGridRows(rows) }

    fun setIconPack(packageName: String?) = viewModelScope.launch {
        preferences.setIconPack(packageName)
    }

    fun setBadges(enabled: Boolean) = viewModelScope.launch {
        preferences.setBadgesEnabled(enabled)
    }

    fun setGesture(slot: GestureSlot, action: GestureAction) = viewModelScope.launch {
        preferences.setGesture(slot, action)
    }

    fun setHidden(app: AppInfo, hidden: Boolean) = viewModelScope.launch {
        customizationRepository.setHidden(app.componentKey, hidden)
    }

    fun writeBackup(uri: Uri) = viewModelScope.launch {
        val json = backupManager.export()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        }
    }

    fun readBackup(uri: Uri) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }
        if (text != null) backupManager.import(text)
    }

    fun setUpdateUrl(url: String) = viewModelScope.launch {
        preferences.setUpdateUrl(url)
    }

    fun checkForUpdates(onMessage: (String) -> Unit) = viewModelScope.launch {
        val manifest = updateRepository.checkForUpdate()
        if (manifest == null) {
            onMessage("You're on the latest version")
            return@launch
        }
        onMessage("Downloading ${manifest.versionName}…")
        val apk = updateRepository.downloadApk(manifest)
        if (apk != null) updateRepository.installApk(apk) else onMessage("Download failed")
    }
}

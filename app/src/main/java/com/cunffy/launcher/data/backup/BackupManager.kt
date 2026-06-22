package com.cunffy.launcher.data.backup

import com.cunffy.launcher.data.db.dao.CustomizationDao
import com.cunffy.launcher.data.db.dao.HomeLayoutDao
import com.cunffy.launcher.data.db.entities.AppCustomizationEntity
import com.cunffy.launcher.data.db.entities.FolderEntity
import com.cunffy.launcher.data.db.entities.FolderItemEntity
import com.cunffy.launcher.data.db.entities.HomeItemEntity
import com.cunffy.launcher.data.prefs.LauncherPreferences
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes the full launcher layout (customizations, home grid, folders, dock, settings)
 * to a JSON string and restores it. The UI handles reading/writing the actual file via SAF.
 */
@Singleton
class BackupManager @Inject constructor(
    private val customizationDao: CustomizationDao,
    private val homeLayoutDao: HomeLayoutDao,
    private val preferences: LauncherPreferences,
) {
    @Serializable
    data class Backup(
        val version: Int = 1,
        val customizations: List<AppCustomizationEntity> = emptyList(),
        val homeItems: List<HomeItemEntity> = emptyList(),
        val folders: List<FolderEntity> = emptyList(),
        val folderItems: List<FolderItemEntity> = emptyList(),
        val dock: List<String> = emptyList(),
        val themedIcons: Boolean = false,
        val iconPack: String? = null,
        val badgesEnabled: Boolean = true,
        val gestures: Map<String, String> = emptyMap(),
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun export(): String {
        val settings = preferences.settings.first()
        val backup = Backup(
            customizations = customizationDao.getAll(),
            homeItems = homeLayoutDao.getItems(),
            folders = homeLayoutDao.getFolders(),
            folderItems = homeLayoutDao.getFolderItems(),
            dock = settings.dockComponents,
            themedIcons = settings.themedIcons,
            iconPack = settings.iconPackPackage,
            badgesEnabled = settings.badgesEnabled,
            gestures = settings.gestures.entries.associate { it.key.name to it.value.name },
        )
        return json.encodeToString(Backup.serializer(), backup)
    }

    suspend fun import(content: String) {
        val backup = json.decodeFromString(Backup.serializer(), content)

        customizationDao.clear()
        backup.customizations.forEach { customizationDao.upsert(it) }

        homeLayoutDao.clearItems()
        homeLayoutDao.clearFolders()
        homeLayoutDao.clearFolderItems()
        backup.folders.forEach { homeLayoutDao.insertFolder(it) }
        backup.folderItems.forEach { homeLayoutDao.insertFolderItem(it) }
        backup.homeItems.forEach { homeLayoutDao.insertItem(it) }

        preferences.setDockComponents(backup.dock)
        preferences.setThemedIcons(backup.themedIcons)
        preferences.setIconPack(backup.iconPack)
        preferences.setBadgesEnabled(backup.badgesEnabled)
        backup.gestures.forEach { (slot, action) ->
            runCatching {
                preferences.setGesture(
                    com.cunffy.launcher.gesture.GestureSlot.valueOf(slot),
                    com.cunffy.launcher.gesture.GestureAction.valueOf(action),
                )
            }
        }
    }
}

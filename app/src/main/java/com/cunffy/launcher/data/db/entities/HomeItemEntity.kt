package com.cunffy.launcher.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Item kinds that can live on the home screen grid. */
object HomeItemType {
    const val APP = "APP"
    const val FOLDER = "FOLDER"
    const val WIDGET = "WIDGET"
}

/** Containers an item can belong to. */
object HomeContainer {
    const val DESKTOP = "DESKTOP"
    const val DOCK = "DOCK"
}

/**
 * One placed item on the home screen (an app shortcut, a folder, or a hosted widget),
 * positioned on a page grid. Folder membership is tracked separately in [FolderItemEntity].
 */
@Serializable
@Entity(tableName = "home_item")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val container: String = HomeContainer.DESKTOP,
    /** For [HomeItemType.APP]: flattened component name. */
    val componentKey: String? = null,
    /** For [HomeItemType.FOLDER]: the folder row id. */
    val folderId: Long? = null,
    /** For [HomeItemType.WIDGET]: the bound AppWidget id. */
    val widgetId: Int? = null,
    val page: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val position: Int = 0,
)

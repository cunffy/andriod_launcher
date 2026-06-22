package com.cunffy.launcher.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "folder")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
)

/** Membership of an app within a folder, with an ordering position. */
@Serializable
@Entity(tableName = "folder_item", primaryKeys = ["folderId", "componentKey"])
data class FolderItemEntity(
    val folderId: Long,
    val componentKey: String,
    val position: Int = 0,
)

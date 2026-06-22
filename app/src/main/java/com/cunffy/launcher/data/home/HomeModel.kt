package com.cunffy.launcher.data.home

import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.db.entities.FolderEntity
import com.cunffy.launcher.data.db.entities.HomeItemEntity

/** A resolved home-screen entry ready to render. */
sealed interface HomeEntry {
    val item: HomeItemEntity

    data class App(override val item: HomeItemEntity, val app: AppInfo) : HomeEntry
    data class Folder(
        override val item: HomeItemEntity,
        val folder: FolderEntity,
        val apps: List<AppInfo>,
    ) : HomeEntry

    data class Widget(override val item: HomeItemEntity) : HomeEntry
}

package com.cunffy.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cunffy.launcher.data.db.dao.CustomizationDao
import com.cunffy.launcher.data.db.dao.HomeLayoutDao
import com.cunffy.launcher.data.db.entities.AppCustomizationEntity
import com.cunffy.launcher.data.db.entities.FolderEntity
import com.cunffy.launcher.data.db.entities.FolderItemEntity
import com.cunffy.launcher.data.db.entities.HomeItemEntity

@Database(
    entities = [
        AppCustomizationEntity::class,
        HomeItemEntity::class,
        FolderEntity::class,
        FolderItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun customizationDao(): CustomizationDao
    abstract fun homeLayoutDao(): HomeLayoutDao
}

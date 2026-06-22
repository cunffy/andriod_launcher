package com.cunffy.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cunffy.launcher.data.db.entities.FolderEntity
import com.cunffy.launcher.data.db.entities.FolderItemEntity
import com.cunffy.launcher.data.db.entities.HomeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeLayoutDao {
    @Query("SELECT * FROM home_item")
    fun observeItems(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_item")
    suspend fun getItems(): List<HomeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HomeItemEntity): Long

    @Update
    suspend fun updateItem(item: HomeItemEntity)

    @Update
    suspend fun updateItems(items: List<HomeItemEntity>)

    @Delete
    suspend fun deleteItem(item: HomeItemEntity)

    @Query("DELETE FROM home_item WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    // Folders
    @Query("SELECT * FROM folder")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder")
    suspend fun getFolders(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folder WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("SELECT * FROM folder_item")
    fun observeFolderItems(): Flow<List<FolderItemEntity>>

    @Query("SELECT * FROM folder_item")
    suspend fun getFolderItems(): List<FolderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderItem(item: FolderItemEntity)

    @Query("DELETE FROM folder_item WHERE folderId = :folderId AND componentKey = :key")
    suspend fun removeFolderItem(folderId: Long, key: String)

    @Query("DELETE FROM folder_item WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)

    @Query("DELETE FROM home_item")
    suspend fun clearItems()

    @Query("DELETE FROM folder")
    suspend fun clearFolders()

    @Query("DELETE FROM folder_item")
    suspend fun clearFolderItems()
}

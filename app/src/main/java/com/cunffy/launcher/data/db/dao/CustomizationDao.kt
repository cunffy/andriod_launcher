package com.cunffy.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cunffy.launcher.data.db.entities.AppCustomizationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomizationDao {
    @Query("SELECT * FROM app_customization")
    fun observeAll(): Flow<List<AppCustomizationEntity>>

    @Query("SELECT * FROM app_customization")
    suspend fun getAll(): List<AppCustomizationEntity>

    @Query("SELECT * FROM app_customization WHERE componentKey = :key")
    suspend fun get(key: String): AppCustomizationEntity?

    @Upsert
    suspend fun upsert(entity: AppCustomizationEntity)

    @Query("DELETE FROM app_customization WHERE componentKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM app_customization")
    suspend fun clear()
}

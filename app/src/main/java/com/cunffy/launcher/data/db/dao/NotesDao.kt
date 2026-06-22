package com.cunffy.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.cunffy.launcher.data.db.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Query("SELECT * FROM note ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE title LIKE :q OR body LIKE :q ORDER BY updatedAt DESC LIMIT 5")
    suspend fun search(q: String): List<NoteEntity>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)
}

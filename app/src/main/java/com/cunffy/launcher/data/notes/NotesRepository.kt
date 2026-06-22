package com.cunffy.launcher.data.notes

import com.cunffy.launcher.data.db.dao.NotesDao
import com.cunffy.launcher.data.db.entities.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Built-in notes store, searchable from universal search. */
@Singleton
class NotesRepository @Inject constructor(
    private val dao: NotesDao,
) {
    val notes: Flow<List<NoteEntity>> = dao.observeAll()

    suspend fun search(query: String): List<NoteEntity> = dao.search("%$query%")

    suspend fun get(id: Long): NoteEntity? = dao.get(id)

    suspend fun save(note: NoteEntity): Long =
        dao.upsert(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(note: NoteEntity) = dao.delete(note)
}

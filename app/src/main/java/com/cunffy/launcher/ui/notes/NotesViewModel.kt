package com.cunffy.launcher.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.db.entities.NoteEntity
import com.cunffy.launcher.data.notes.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
) : ViewModel() {

    val notes = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun get(id: Long): NoteEntity? = repository.get(id)

    fun save(id: Long, title: String, body: String) = viewModelScope.launch {
        if (title.isBlank() && body.isBlank()) return@launch
        repository.save(NoteEntity(id = id, title = title, body = body))
    }

    fun delete(note: NoteEntity) = viewModelScope.launch { repository.delete(note) }
}

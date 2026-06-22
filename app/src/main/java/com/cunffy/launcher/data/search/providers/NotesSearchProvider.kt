package com.cunffy.launcher.data.search.providers

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.StickyNote2
import com.cunffy.launcher.data.notes.NotesRepository
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import com.cunffy.launcher.ui.notes.NotesActivity
import javax.inject.Inject

/** Surfaces matching notes and a "create note" shortcut from search. */
class NotesSearchProvider @Inject constructor(
    private val notesRepository: NotesRepository,
) : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        if (query.length < 2) return emptyList()
        val matches = notesRepository.search(query).map { note ->
            SearchResult(
                id = "note:${note.id}",
                title = note.title.ifBlank { "Untitled note" },
                subtitle = note.body.take(60).ifBlank { "Note" },
                type = SearchResultType.NOTE,
                icon = ResultIcon.OfVector(Icons.Rounded.StickyNote2),
                score = 60,
                onActivate = { context ->
                    context.startActivity(
                        Intent(context, NotesActivity::class.java)
                            .putExtra(NotesActivity.EXTRA_NOTE_ID, note.id)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        }
        val create = SearchResult(
            id = "note:create",
            title = "Create note “$query”",
            subtitle = "Notes",
            type = SearchResultType.NOTE,
            icon = ResultIcon.OfVector(Icons.Rounded.Add),
            score = 10,
            onActivate = { context ->
                context.startActivity(
                    Intent(context, NotesActivity::class.java)
                        .putExtra(NotesActivity.EXTRA_NEW_BODY, query)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
        return matches + create
    }
}

package com.cunffy.launcher.ui.notes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.cunffy.launcher.ui.theme.LauncherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotesActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it >= 0 }
        val newBody = intent.getStringExtra(EXTRA_NEW_BODY)
        setContent {
            LauncherTheme {
                NotesScreen(
                    initialNoteId = noteId,
                    initialNewBody = newBody,
                    onBack = ::finish,
                )
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NEW_BODY = "new_body"
    }
}

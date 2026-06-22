package com.cunffy.launcher.data.search.providers

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.core.content.ContextCompat
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Searches device contacts by display name; opens the contact card on tap. */
class ContactsSearchProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        if (query.length < 2 || !hasPermission()) return emptyList()
        return withContext(Dispatchers.IO) { runQuery(query) }
    }

    private fun runQuery(query: String): List<SearchResult> {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
        )
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
            )
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("%$query%"))
            putInt(ContentResolver.QUERY_ARG_LIMIT, MAX_RESULTS)
        }

        val results = mutableListOf<SearchResult>()
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, projection, args, null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
                results += SearchResult(
                    id = "contact:$id",
                    title = name,
                    subtitle = "Contact",
                    type = SearchResultType.CONTACT,
                    icon = ResultIcon.OfVector(Icons.Rounded.Person),
                    score = 70,
                    onActivate = { ctx ->
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, uri)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        }
        return results
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val MAX_RESULTS = 5
    }
}

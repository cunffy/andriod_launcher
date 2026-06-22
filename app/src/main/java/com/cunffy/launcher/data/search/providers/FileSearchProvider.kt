package com.cunffy.launcher.data.search.providers

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Searches on-device media via [MediaStore]. Gated on the runtime media-read
 * permission; returns nothing (rather than crashing) until the user grants it.
 */
class FileSearchProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        if (query.length < 2 || !hasMediaPermission()) return emptyList()
        return withContext(Dispatchers.IO) { runQuery(query) }
    }

    private fun runQuery(query: String): List<SearchResult> {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf("%$query%"),
            )
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, MAX_RESULTS)
        }

        val results = mutableListOf<SearchResult>()
        context.contentResolver.query(collection, projection, args, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol) ?: "*/*"
                val uri = ContentUris.withAppendedId(collection, id)
                results += SearchResult(
                    id = "file:$id",
                    title = name,
                    subtitle = mime,
                    type = SearchResultType.FILE,
                    icon = ResultIcon.OfVector(iconForMime(mime)),
                    score = 50,
                    onActivate = { ctx ->
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, mime)
                                .addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_ACTIVITY_NEW_TASK,
                                ),
                        )
                    },
                )
            }
        }
        return results
    }

    private fun hasMediaPermission(): Boolean {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return perms.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun iconForMime(mime: String): ImageVector = when {
        mime.startsWith("image/") -> Icons.Rounded.Image
        mime.startsWith("video/") -> Icons.Rounded.Movie
        mime.startsWith("audio/") -> Icons.Rounded.AudioFile
        else -> Icons.Rounded.InsertDriveFile
    }

    private companion object {
        const val MAX_RESULTS = 8
    }
}

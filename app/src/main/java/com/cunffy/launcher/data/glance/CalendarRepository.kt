package com.cunffy.launcher.data.glance

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GlanceEvent(val title: String, val beginMillis: Long)

/** Reads the next upcoming calendar event for the At-a-Glance strip. */
@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun nextEvent(): GlanceEvent? {
        if (!hasPermission()) return null
        return withContext(Dispatchers.IO) { runQuery() }
    }

    private fun runQuery(): GlanceEvent? {
        val now = System.currentTimeMillis()
        val end = now + WINDOW_MS
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
        )
        return context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            if (cursor.moveToFirst()) {
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Event"
                GlanceEvent(title, cursor.getLong(beginCol))
            } else {
                null
            }
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}

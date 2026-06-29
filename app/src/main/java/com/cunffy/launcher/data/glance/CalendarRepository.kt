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

/** [id] is the calendar EVENT_ID so a chosen event can be pinned across instances. */
data class GlanceEvent(val id: Long, val title: String, val beginMillis: Long)

/** Reads upcoming calendar events for the At-a-Glance strip. */
@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** The very next event within the next week. */
    suspend fun nextEvent(): GlanceEvent? {
        if (!hasPermission()) return null
        return withContext(Dispatchers.IO) { query(WEEK_MS).firstOrNull() }
    }

    /** Upcoming events over the next year, for the "choose an event" picker. */
    suspend fun upcomingEvents(limit: Int = 30): List<GlanceEvent> {
        if (!hasPermission()) return emptyList()
        return withContext(Dispatchers.IO) { query(YEAR_MS).distinctById().take(limit) }
    }

    /** The next instance of a specific pinned event (by EVENT_ID), within the next year. */
    suspend fun eventById(eventId: Long): GlanceEvent? {
        if (!hasPermission()) return null
        return withContext(Dispatchers.IO) { query(YEAR_MS).firstOrNull { it.id == eventId } }
    }

    private fun query(windowMs: Long): List<GlanceEvent> {
        val now = System.currentTimeMillis()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, now + windowMs)
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
        )
        val result = ArrayList<GlanceEvent>()
        context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Event"
                result.add(GlanceEvent(cursor.getLong(idCol), title, cursor.getLong(beginCol)))
            }
        }
        return result
    }

    /** Collapse recurring events to their next instance for the picker list. */
    private fun List<GlanceEvent>.distinctById(): List<GlanceEvent> {
        val seen = HashSet<Long>()
        return filter { seen.add(it.id) }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
        const val YEAR_MS = 365L * 24 * 60 * 60 * 1000
    }
}

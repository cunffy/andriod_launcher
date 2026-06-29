package com.cunffy.launcher.ui.home

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.data.glance.GlanceEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pixel-style "At a Glance" strip shown under the clock: weather (tap to open a weather app)
 * and a calendar event. Tap the event to open the calendar; long-press it to choose which
 * event to show and toggle a day countdown. Renders nothing when there's nothing to show.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtAGlance(
    modifier: Modifier = Modifier,
    viewModel: GlanceViewModel = hiltViewModel(),
) {
    // Re-fetch periodically so it fills in once location/calendar permissions are granted and
    // stays current as weather and the next event change.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            kotlinx.coroutines.delay(60_000)
        }
    }
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    val weather = state.weather
    val event = state.event
    if (weather == null && event == null) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (weather != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { openWeather(context) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WbSunny, contentDescription = "Weather", tint = Color.White)
                Text(
                    text = "${weather.temperatureC}° · ${weather.description}",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
        if (event != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { openCalendarAt(context, event.beginMillis) },
                        onLongClick = { viewModel.loadUpcoming(); showPicker = true },
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Event, contentDescription = "Calendar", tint = Color.White)
                Text(
                    text = eventLabel(event, state.countdown),
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
    }

    if (showPicker) {
        EventPickerDialog(
            viewModel = viewModel,
            countdownOn = state.countdown,
            selectedId = event?.id ?: -1L,
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun EventPickerDialog(
    viewModel: GlanceViewModel,
    countdownOn: Boolean,
    selectedId: Long,
    onDismiss: () -> Unit,
) {
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Glance event") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Show countdown", modifier = Modifier.weight(1f))
                    Switch(checked = countdownOn, onCheckedChange = { viewModel.setCountdown(it) })
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Next event (automatic)",
                    color = if (selectedId < 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.useNextEvent(); onDismiss() }
                        .padding(vertical = 10.dp),
                )
                if (upcoming.isEmpty()) {
                    Text(
                        text = "No upcoming events found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(upcoming, key = { it.id }) { item ->
                        Text(
                            text = "${item.title} · ${DATE.format(Date(item.beginMillis))}",
                            color = if (item.id == selectedId) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.pinEvent(item.id); onDismiss() }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
    )
}

/** Either "h:mm a · Title" or, with countdown on, "Title · in N days" / "Today" / "Tomorrow". */
private fun eventLabel(event: GlanceEvent, countdown: Boolean): String {
    if (!countdown) return "${TIME.format(Date(event.beginMillis))} · ${event.title}"
    val days = daysUntil(event.beginMillis)
    val when_ = when {
        days <= 0L -> "Today"
        days == 1L -> "Tomorrow"
        else -> "in $days days"
    }
    return "${event.title} · $when_"
}

/** Whole calendar days from today (midnight) to the event's day. */
private fun daysUntil(beginMillis: Long): Long {
    fun midnight(millis: Long) = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val today = midnight(System.currentTimeMillis())
    val target = midnight(beginMillis)
    return (target - today) / (24L * 60 * 60 * 1000)
}

/** Open the calendar app at the event's time; fall back to just launching the calendar. */
private fun openCalendarAt(context: Context, beginMillis: Long) {
    val timeUri = CalendarContract.CONTENT_URI.buildUpon()
        .appendPath("time")
        .let { ContentUris.appendId(it, beginMillis); it.build() }
    val view = Intent(Intent.ACTION_VIEW, timeUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(view); true }.getOrDefault(false)) return
    val fallback = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(fallback) }
}

/** Open an installed weather app if we can find one, else a weather web search. */
private fun openWeather(context: Context) {
    val pm = context.packageManager
    val candidates = listOf(
        "com.google.android.apps.weather",         // Google Weather (Pixel)
        "com.miui.weather2",                        // Xiaomi
        "com.sec.android.daemonapp",                // Samsung
        "com.accuweather.android",
        "com.weather.Weather",                      // The Weather Channel
        "com.google.android.googlequicksearchbox",  // Google app (has weather)
    )
    for (pkg in candidates) {
        val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
        if (runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }
                .getOrDefault(false)
        ) {
            return
        }
    }
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(web) }
}

private val TIME = SimpleDateFormat("h:mm a", Locale.getDefault())
private val DATE = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

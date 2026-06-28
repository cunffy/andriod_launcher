package com.cunffy.launcher.ui.home

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pixel-style "At a Glance" strip shown under the clock: weather (when a weather provider is
 * configured) and the next calendar event. Tapping weather opens a weather app; tapping the
 * event opens the calendar at that time. Renders nothing when there's nothing to show.
 */
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
                    .clickable { openCalendarAt(context, event.beginMillis) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Event, contentDescription = "Calendar", tint = Color.White)
                Text(
                    text = "${TIME.format(Date(event.beginMillis))} · ${event.title}",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
    }
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
        "com.google.android.apps.weather",      // Google Weather (Pixel)
        "com.miui.weather2",                     // Xiaomi
        "com.sec.android.daemonapp",             // Samsung
        "com.accuweather.android",
        "com.weather.Weather",                   // The Weather Channel
        "com.google.android.googlequicksearchbox", // Google app (has weather)
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

package com.cunffy.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pixel-style "At a Glance" strip shown under the clock: weather (when a weather provider is
 * configured) and the next calendar event. Renders nothing when there's nothing to show.
 */
@Composable
fun AtAGlance(
    modifier: Modifier = Modifier,
    viewModel: GlanceViewModel = hiltViewModel(),
) {
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
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = Color.White)
            Text(
                text = "${weather.temperatureC}° · ${weather.description}",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
        if (event != null) {
            Icon(Icons.Rounded.Event, contentDescription = null, tint = Color.White)
            Text(
                text = "${TIME.format(Date(event.beginMillis))} · ${event.title}",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}

private val TIME = SimpleDateFormat("h:mm a", Locale.getDefault())

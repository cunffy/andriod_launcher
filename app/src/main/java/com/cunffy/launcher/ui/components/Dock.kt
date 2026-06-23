package com.cunffy.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.data.apps.AppInfo

/** Fixed bottom row of favorite apps; icons shrink to fit so a full row never overflows. */
@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 52.dp,
) {
    if (apps.isEmpty()) return
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // Each app gets an equal slice; the icon is capped to fit that slice.
        val perCell = maxWidth / apps.size
        val resolved = if (perCell - 8.dp < iconSize) (perCell - 8.dp) else iconSize
        Row(modifier = Modifier.fillMaxWidth()) {
            apps.forEach { app ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AppIcon(
                        app = app,
                        onClick = { onAppClick(app) },
                        showLabel = false,
                        labelColor = Color.White,
                        iconSize = resolved,
                    )
                }
            }
        }
    }
}

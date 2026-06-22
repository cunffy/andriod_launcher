package com.cunffy.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.home.HomeEntry
import com.cunffy.launcher.ui.components.AppIcon
import com.cunffy.launcher.ui.components.rememberDrawablePainter
import com.cunffy.launcher.widgets.HostedWidget
import com.cunffy.launcher.widgets.WidgetHostController
import androidx.compose.foundation.Image
import kotlin.math.roundToInt

/** A single positioned home item; draggable in edit mode, with a remove affordance. */
@Composable
fun HomeItemView(
    entry: HomeEntry,
    cellW: Dp,
    cellH: Dp,
    cellWpx: Float,
    cellHpx: Float,
    columns: Int,
    rows: Int,
    editMode: Boolean,
    controller: WidgetHostController,
    badgeCount: Int,
    onLaunchApp: (AppInfo) -> Unit,
    onLongClickApp: (AppInfo) -> Unit,
    onOpenFolder: (HomeEntry.Folder) -> Unit,
    onRemove: () -> Unit,
    onDropped: (Int, Int) -> Unit,
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var drag by remember(entry.item.id) { mutableStateOf(Offset.Zero) }
    val spanX = entry.item.spanX
    val spanY = entry.item.spanY
    val baseX = entry.item.cellX * cellWpx
    val baseY = entry.item.cellY * cellHpx

    Box(
        modifier = Modifier
            .offset { IntOffset((baseX + drag.x).roundToInt(), (baseY + drag.y).roundToInt()) }
            .size(cellW * spanX, cellH * spanY)
            .then(
                if (editMode) {
                    Modifier.pointerInput(entry.item.id) {
                        detectDragGestures(
                            onDragStart = {
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                                )
                            },
                            onDrag = { change, delta -> drag += delta; change.consume() },
                            onDragEnd = {
                                val nx = ((baseX + drag.x) / cellWpx).roundToInt()
                                    .coerceIn(0, (columns - spanX).coerceAtLeast(0))
                                val ny = ((baseY + drag.y) / cellHpx).roundToInt()
                                    .coerceIn(0, (rows - spanY).coerceAtLeast(0))
                                drag = Offset.Zero
                                onDropped(nx, ny)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (entry) {
            is HomeEntry.App -> AppIcon(
                app = entry.app,
                onClick = { onLaunchApp(entry.app) },
                onLongClick = if (!editMode) {
                    { onLongClickApp(entry.app) }
                } else {
                    null
                },
                labelColor = Color.White,
                badgeCount = badgeCount,
            )
            is HomeEntry.Folder -> FolderTile(
                folder = entry,
                onClick = { onOpenFolder(entry) },
            )
            is HomeEntry.Widget -> HostedWidget(
                controller = controller,
                widgetId = entry.item.widgetId ?: 0,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        }

        if (editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Folder shown on the grid: up to four app icons in a tile, with a label. */
@Composable
private fun FolderTile(folder: HomeEntry.Folder, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .clickable(onClick = onClick)
                .padding(6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                folder.apps.chunked(2).take(2).forEach { rowApps ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        rowApps.take(2).forEach { app ->
                            Image(
                                painter = rememberDrawablePainter(app.icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = folder.folder.title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

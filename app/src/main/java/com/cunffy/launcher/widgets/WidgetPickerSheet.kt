package com.cunffy.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.ui.components.rememberDrawablePainter

/** One app's widgets, grouped under its name for the picker. */
private data class WidgetGroup(
    val packageName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>,
)

/**
 * Pixel-style widget picker: a scannable list of apps, each collapsed to one row showing its
 * icon, name, and widget count. Tap an app to expand its widgets (live preview + grid size);
 * tap a widget to place it. A search field filters by app or widget name and auto-expands hits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    controller: WidgetHostController,
    onDismiss: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val groups = remember {
        controller.installedProviders()
            .groupBy { it.provider.packageName }
            .map { (pkg, widgets) ->
                val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                WidgetGroup(
                    packageName = pkg,
                    appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg,
                    appIcon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() },
                    widgets = widgets.sortedBy { it.loadLabel(pm).orEmpty() },
                )
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(query, groups) {
        if (query.isBlank()) {
            groups
        } else {
            groups.mapNotNull { group ->
                val appMatches = group.appLabel.contains(query, ignoreCase = true)
                if (appMatches) {
                    group
                } else {
                    val hits = group.widgets.filter {
                        it.loadLabel(pm).orEmpty().contains(query, ignoreCase = true)
                    }
                    if (hits.isEmpty()) null else group.copy(widgets = hits)
                }
            }
        }
    }

    // Which apps are expanded. While searching, every shown group is expanded automatically.
    var expanded by remember { mutableStateOf(setOf<String>()) }
    val searching = query.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Widgets",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search widgets") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (filtered.isEmpty()) {
            Text(
                text = if (searching) "No widgets match \"$query\"." else
                    "No widgets are available on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.packageName }) { group ->
                val isOpen = searching || group.packageName in expanded
                WidgetGroupCard(
                    group = group,
                    expanded = isOpen,
                    onToggle = {
                        expanded = if (group.packageName in expanded) {
                            expanded - group.packageName
                        } else {
                            expanded + group.packageName
                        }
                    },
                    labelOf = { it.loadLabel(pm).orEmpty() },
                    previewOf = { it.loadPreviewOrIcon(context) },
                    onPick = onPick,
                )
            }
        }
    }
}

@Composable
private fun WidgetGroupCard(
    group: WidgetGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    labelOf: (AppWidgetProviderInfo) -> String,
    previewOf: (AppWidgetProviderInfo) -> Drawable?,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
            ) {
                group.appIcon?.let {
                    Image(
                        painter = rememberDrawablePainter(it),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${group.widgets.size} widget${if (group.widgets.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Two-up grid of preview tiles — more scannable than a tall single column.
                    group.widgets.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { widget ->
                                WidgetTile(
                                    label = labelOf(widget),
                                    sizeLabel = "${widget.cellWidth()}×${widget.cellHeight()}",
                                    preview = previewOf(widget),
                                    onClick = { onPick(widget) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetTile(
    label: String,
    sizeLabel: String,
    preview: Drawable?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    Image(
                        painter = rememberDrawablePainter(preview),
                        contentDescription = label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                    )
                } else {
                    Text(
                        text = label.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$sizeLabel grid",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val CELL_DP = 70

private fun AppWidgetProviderInfo.cellWidth(): Int = (minWidth / CELL_DP + 1).coerceAtLeast(1)
private fun AppWidgetProviderInfo.cellHeight(): Int = (minHeight / CELL_DP + 1).coerceAtLeast(1)

private fun AppWidgetProviderInfo.loadPreviewOrIcon(context: android.content.Context): Drawable? =
    runCatching { loadPreviewImage(context, 0) }.getOrNull()
        ?: runCatching { loadIcon(context, 0) }.getOrNull()

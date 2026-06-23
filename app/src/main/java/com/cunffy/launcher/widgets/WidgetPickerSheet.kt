package com.cunffy.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.ui.components.rememberDrawablePainter

/** One app's widgets, grouped under its name for the picker. */
private data class WidgetGroup(
    val appLabel: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>,
)

/**
 * A clean, Pixel-style widget picker: every installed widget grouped under its app, with a
 * live preview, name, and grid size. Tapping a widget hands it back to be placed.
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
                    appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg,
                    appIcon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() },
                    widgets = widgets.sortedBy { it.loadLabel(pm).orEmpty() },
                )
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Widgets",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        if (groups.isEmpty()) {
            Text(
                text = "No widgets are available on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            groups.forEach { group ->
                item {
                    WidgetGroupSection(
                        group = group,
                        labelOf = { it.loadLabel(pm).orEmpty() },
                        previewOf = { it.loadPreviewOrIcon(context) },
                        onPick = onPick,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetGroupSection(
    group: WidgetGroup,
    labelOf: (AppWidgetProviderInfo) -> String,
    previewOf: (AppWidgetProviderInfo) -> Drawable?,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 4.dp),
        ) {
            group.appIcon?.let {
                Image(
                    painter = rememberDrawablePainter(it),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            Text(
                text = group.appLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        group.widgets.forEach { widget ->
            WidgetRow(
                label = labelOf(widget),
                sizeLabel = "${widget.cellWidth()}×${widget.cellHeight()}",
                preview = previewOf(widget)?.let { rememberDrawablePainter(it) },
                onClick = { onPick(widget) },
            )
        }
    }
}

@Composable
private fun WidgetRow(
    label: String,
    sizeLabel: String,
    preview: Painter?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    Image(
                        painter = preview,
                        contentDescription = label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = sizeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val CELL_DP = 70

private fun AppWidgetProviderInfo.cellWidth(): Int = (minWidth / CELL_DP + 1).coerceAtLeast(1)
private fun AppWidgetProviderInfo.cellHeight(): Int = (minHeight / CELL_DP + 1).coerceAtLeast(1)

private fun AppWidgetProviderInfo.loadPreviewOrIcon(context: android.content.Context): Drawable? =
    runCatching { loadPreviewImage(context, 0) }.getOrNull()
        ?: runCatching { loadIcon(context, 0) }.getOrNull()

package com.cunffy.launcher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.cunffy.launcher.data.apps.AppInfo

/** Caches a bitmap render of an app's [Drawable] so it isn't rasterized every recomposition. */
@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter = remember(drawable) {
    val size = (drawable.intrinsicWidth.takeIf { it > 0 } ?: ICON_RENDER_PX)
    BitmapPainter(drawable.toBitmap(size, size).asImageBitmap())
}

private const val ICON_RENDER_PX = 144

/** Icon + label tile used in the dock, drawer grid, and search results. */
@Composable
fun AppIcon(
    app: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    showLabel: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 52.dp,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Image(
            painter = rememberDrawablePainter(app.icon),
            contentDescription = app.label,
            modifier = Modifier.size(iconSize),
        )
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

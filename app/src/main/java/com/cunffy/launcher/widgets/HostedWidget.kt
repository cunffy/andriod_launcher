package com.cunffy.launcher.widgets

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView

/** Renders a bound AppWidget by id, embedding the hosted [android.appwidget.AppWidgetHostView]. */
@Composable
fun HostedWidget(
    controller: WidgetHostController,
    widgetId: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    AndroidView(
        // Report the laid-out size to the widget so it picks the correct responsive layout
        // (without this many widgets render clipped or at their minimum size).
        modifier = modifier.onSizeChanged { size ->
            with(density) {
                controller.updateSize(
                    widgetId,
                    size.width.toDp().value.toInt(),
                    size.height.toDp().value.toInt(),
                )
            }
        },
        factory = { ctx ->
            (controller.createView(ctx, widgetId) ?: android.widget.FrameLayout(ctx)).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
    )
}

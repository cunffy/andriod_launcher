package com.cunffy.launcher.widgets

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Renders a bound AppWidget by id, embedding the hosted [android.appwidget.AppWidgetHostView]. */
@Composable
fun HostedWidget(
    controller: WidgetHostController,
    widgetId: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            controller.createView(ctx, widgetId) ?: android.widget.FrameLayout(ctx)
        },
        update = { view ->
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        },
    )
}

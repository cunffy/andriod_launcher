package com.cunffy.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cunffy.launcher.gesture.NotificationShade
import com.cunffy.launcher.ui.drawer.AppDrawerScreen
import com.cunffy.launcher.ui.home.HomeScreen
import kotlinx.coroutines.launch

/**
 * Top-level launcher surface. The home screen sits behind a full-screen app-drawer sheet
 * whose vertical position is driven by [progress] (1 = fully closed/off-screen, 0 = open):
 *
 *  - swipe up on home → open drawer
 *  - swipe down on home → expand the notification shade
 *  - drag the handle down / Back / Home button → close drawer
 */
@Composable
fun LauncherRoot(homePressTick: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var heightPx by remember { mutableFloatStateOf(0f) }
    // 1f = closed, 0f = open. Animatable so drag (snapTo) and settle (animateTo) share state.
    val progress = remember { Animatable(1f) }

    fun open() = scope.launch { progress.animateTo(0f, tween(300)) }
    fun close() = scope.launch { progress.animateTo(1f, tween(300)) }

    // Pressing the Home button re-delivers a HOME intent; collapse the drawer when it does.
    LaunchedEffect(homePressTick) {
        if (homePressTick > 0) progress.animateTo(1f, tween(250))
    }

    BackHandler(enabled = progress.value < 0.999f) { close() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat() },
    ) {
        var dragAccum by remember { mutableFloatStateOf(0f) }
        HomeScreen(
            onOpenDrawer = { open() },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(heightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onVerticalDrag = { _, dy ->
                            dragAccum += dy
                            // Only an upward drag pulls the drawer up 1:1.
                            if (heightPx > 0f && dy < 0f) {
                                val next = (progress.value + dy / heightPx).coerceIn(0f, 1f)
                                scope.launch { progress.snapTo(next) }
                            }
                        },
                        onDragEnd = {
                            when {
                                dragAccum < -heightPx * SETTLE_FRACTION -> open()
                                dragAccum > heightPx * SETTLE_FRACTION -> {
                                    NotificationShade.expand(context)
                                    close()
                                }
                                else -> close()
                            }
                        },
                    )
                },
        )

        if (progress.value < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = progress.value * heightPx }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    DragHandle(
                        onDrag = { dy ->
                            if (heightPx > 0f) {
                                val next = (progress.value + dy / heightPx).coerceIn(0f, 1f)
                                scope.launch { progress.snapTo(next) }
                            }
                        },
                        onDragEnd = { if (progress.value > CLOSE_THRESHOLD) close() else open() },
                    )
                    AppDrawerScreen(
                        onRequestClose = { close() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dy -> onDrag(dy) },
                    onDragEnd = onDragEnd,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
        )
    }
}

private const val SETTLE_FRACTION = 0.15f
private const val CLOSE_THRESHOLD = 0.35f

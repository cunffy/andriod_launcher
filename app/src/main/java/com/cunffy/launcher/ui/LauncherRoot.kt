package com.cunffy.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.gesture.GestureAction
import com.cunffy.launcher.gesture.GestureSlot
import com.cunffy.launcher.gesture.NotificationShade
import com.cunffy.launcher.ui.drawer.AppDrawerScreen
import com.cunffy.launcher.ui.home.HomeScreen
import com.cunffy.launcher.ui.onboarding.OnboardingScreen
import com.cunffy.launcher.ui.settings.SettingsActivity
import com.cunffy.launcher.ui.update.UpdateHost
import com.cunffy.launcher.ui.update.UpdateViewModel
import android.content.Intent
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
fun LauncherRoot(homePressTick: Int, viewModel: LauncherViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
    var heightPx by remember { mutableFloatStateOf(0f) }
    // 1f = closed, 0f = open. Animatable so drag (snapTo) and settle (animateTo) share state.
    val progress = remember { Animatable(1f) }

    val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    fun open() = scope.launch { progress.animateTo(0f, settleSpec) }
    fun close() = scope.launch { progress.animateTo(1f, settleSpec) }

    fun performGesture(action: GestureAction) {
        when (action) {
            GestureAction.OPEN_DRAWER, GestureAction.OPEN_SEARCH -> open()
            GestureAction.EXPAND_NOTIFICATIONS -> NotificationShade.expand(context)
            GestureAction.EXPAND_QUICK_SETTINGS -> NotificationShade.expandQuickSettings(context)
            GestureAction.OPEN_LAUNCHER_SETTINGS -> context.startActivity(
                Intent(context, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            GestureAction.NONE -> Unit
        }
    }

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
        // Optional dim over the live wallpaper for readability.
        if (settings.wallpaperDim > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = settings.wallpaperDim / 100f)),
            )
        }

        var dragAccum by remember { mutableFloatStateOf(0f) }
        val swipeUp = settings.gestures[GestureSlot.SWIPE_UP] ?: GestureAction.OPEN_DRAWER
        val swipeDown = settings.gestures[GestureSlot.SWIPE_DOWN] ?: GestureAction.EXPAND_NOTIFICATIONS
        val doubleTap = settings.gestures[GestureSlot.DOUBLE_TAP] ?: GestureAction.NONE
        // Only drag the drawer up 1:1 when swipe-up is actually bound to opening it.
        val dragOpensDrawer = swipeUp == GestureAction.OPEN_DRAWER || swipeUp == GestureAction.OPEN_SEARCH
        HomeScreen(
            onOpenDrawer = { open() },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(doubleTap) {
                    detectTapGestures(onDoubleTap = { performGesture(doubleTap) })
                }
                .pointerInput(heightPx, dragOpensDrawer) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onVerticalDrag = { _, dy ->
                            dragAccum += dy
                            if (dragOpensDrawer && heightPx > 0f && dy < 0f) {
                                val next = (progress.value + dy / heightPx).coerceIn(0f, 1f)
                                scope.launch { progress.snapTo(next) }
                            }
                        },
                        onDragEnd = {
                            when {
                                dragAccum < -heightPx * SETTLE_FRACTION -> {
                                    if (dragOpensDrawer) open() else { performGesture(swipeUp); close() }
                                }
                                dragAccum > heightPx * SETTLE_FRACTION -> {
                                    performGesture(swipeDown)
                                    close()
                                }
                                else -> close()
                            }
                        },
                    )
                },
        )

        // Keep the drawer composed once the size is known and just translate it; this avoids
        // re-composing the whole grid every open, which is what made opening feel slow.
        if (heightPx > 0f) {
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

        // In-app update prompt (checks once per process for a newer build).
        val updateViewModel: UpdateViewModel = hiltViewModel()
        LaunchedEffect(Unit) { updateViewModel.checkOnLaunch() }
        UpdateHost(viewModel = updateViewModel)

        // First-run onboarding overlays everything until completed.
        if (onboardingComplete == false) {
            OnboardingScreen()
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

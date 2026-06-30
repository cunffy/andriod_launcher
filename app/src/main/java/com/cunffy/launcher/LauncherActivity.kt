package com.cunffy.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.cunffy.launcher.data.prefs.LauncherPreferences
import com.cunffy.launcher.ui.LauncherRoot
import com.cunffy.launcher.ui.theme.LauncherThemeGate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host. Because the activity is the system HOME target with
 * launchMode="singleTask", pressing the Home button re-delivers a HOME intent to
 * the running instance via [onNewIntent] rather than recreating it — we use that
 * to collapse the app drawer back to the home screen.
 */
@AndroidEntryPoint
class LauncherActivity : FragmentActivity() {

    @Inject lateinit var preferences: LauncherPreferences

    // Incremented each time HOME is pressed so Compose can react and collapse the drawer.
    private val homePressTick = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Honor the high-refresh-rate preference (off by default — forcing the panel to its max
        // rate on a mostly-static home screen wastes battery, especially on 144Hz RedMagic
        // displays). The system otherwise ramps the rate up during animations on its own.
        lifecycleScope.launch {
            preferences.settings
                .map { it.highRefreshRate }
                .distinctUntilChanged()
                .collect { applyRefreshRate(it) }
        }
        setContent {
            LauncherThemeGate {
                LauncherRoot(homePressTick = homePressTick.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val isHome = intent.hasCategory(Intent.CATEGORY_HOME)
        if (isHome) {
            homePressTick.value += 1
        }
    }
}

/**
 * Refresh-rate policy for the launcher window. When high refresh is OFF we impose *nothing* —
 * no mode pin and no rate hint — so the system's own "auto refresh rate" fully manages it and
 * can idle the (static) home screen down. Crucially we never pin a display *mode*: pinning a
 * mode can latch some panels (e.g. RedMagic) into that rate device-wide until reboot. When the
 * user opts into high refresh we only add a soft max-rate hint.
 */
fun Activity.applyRefreshRate(enabled: Boolean) {
    val params = window.attributes
    // Always clear any pinned mode (undoes the behavior of older builds).
    var changed = false
    if (params.preferredDisplayModeId != 0) {
        params.preferredDisplayModeId = 0
        changed = true
    }
    val targetRate = if (enabled) {
        val display = display ?: return
        val current = display.mode ?: return
        display.supportedModes
            .filter {
                it.physicalWidth == current.physicalWidth &&
                    it.physicalHeight == current.physicalHeight
            }
            .maxOfOrNull { it.refreshRate } ?: 0f
    } else {
        0f // no preference — let the system's auto refresh decide.
    }
    if (params.preferredRefreshRate != targetRate) {
        params.preferredRefreshRate = targetRate
        changed = true
    }
    if (changed) window.attributes = params
}

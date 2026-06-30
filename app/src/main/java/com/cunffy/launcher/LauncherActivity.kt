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
 * Hint the launcher window's refresh rate: the panel's max when [enabled], otherwise its lowest
 * (≈60Hz). We use [WindowManager.LayoutParams.preferredRefreshRate] rather than pinning a full
 * display mode so it cooperates with the system's "auto refresh rate" instead of fighting it —
 * with high-refresh off and a now-idle home screen, the system can keep the rate low.
 */
fun Activity.applyRefreshRate(enabled: Boolean) {
    val display = display ?: return
    val current = display.mode ?: return
    val rates = display.supportedModes
        .filter {
            it.physicalWidth == current.physicalWidth &&
                it.physicalHeight == current.physicalHeight
        }
        .map { it.refreshRate }
    if (rates.isEmpty()) return
    val rate = if (enabled) {
        rates.max()
    } else {
        rates.filter { it >= 59f }.minOrNull() ?: rates.min()
    }
    val params = window.attributes
    if (params.preferredRefreshRate != rate || params.preferredDisplayModeId != 0) {
        params.preferredRefreshRate = rate
        // Don't also pin a mode — the rate hint above lets auto-refresh do its job.
        params.preferredDisplayModeId = 0
        window.attributes = params
    }
}

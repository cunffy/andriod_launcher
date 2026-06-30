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
 * When [enabled], request the highest refresh-rate mode at the current resolution; otherwise
 * release the request and let the system manage the rate (the battery-friendly default).
 */
fun Activity.applyRefreshRate(enabled: Boolean) {
    val params = window.attributes
    if (!enabled) {
        if (params.preferredDisplayModeId != 0) {
            window.attributes = params.apply { preferredDisplayModeId = 0 }
        }
        return
    }
    val display = display ?: return
    val current = display.mode ?: return
    val best = display.supportedModes
        .filter {
            it.physicalWidth == current.physicalWidth &&
                it.physicalHeight == current.physicalHeight
        }
        .maxByOrNull { it.refreshRate } ?: return
    if (best.modeId != params.preferredDisplayModeId) {
        window.attributes = params.apply { preferredDisplayModeId = best.modeId }
    }
}

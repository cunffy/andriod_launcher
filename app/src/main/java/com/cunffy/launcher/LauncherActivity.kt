package com.cunffy.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import com.cunffy.launcher.ui.LauncherRoot
import com.cunffy.launcher.ui.theme.LauncherThemeGate
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Because the activity is the system HOME target with
 * launchMode="singleTask", pressing the Home button re-delivers a HOME intent to
 * the running instance via [onNewIntent] rather than recreating it — we use that
 * to collapse the app drawer back to the home screen.
 */
@AndroidEntryPoint
class LauncherActivity : FragmentActivity() {

    // Incremented each time HOME is pressed so Compose can react and collapse the drawer.
    private val homePressTick = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

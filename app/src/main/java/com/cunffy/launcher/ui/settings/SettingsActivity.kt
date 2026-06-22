package com.cunffy.launcher.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cunffy.launcher.ui.theme.LauncherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = when (intent.getStringExtra(EXTRA_DESTINATION)) {
            DEST_HIDDEN -> ROUTE_HIDDEN
            else -> ROUTE_ROOT
        }
        setContent {
            LauncherTheme {
                SettingsNavHost(startDestination = start, onFinish = ::finish)
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DEST_ROOT = "root"
        const val DEST_APPEARANCE = "appearance"
        const val DEST_GESTURES = "gestures"
        const val DEST_HIDDEN = "hidden"
        const val DEST_BACKUP = "backup"
    }
}

private const val ROUTE_ROOT = "settings_root"
private const val ROUTE_HIDDEN = "settings_hidden"

@Composable
private fun SettingsNavHost(startDestination: String, onFinish: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_ROOT) {
            SettingsScreen(
                onOpenHiddenApps = { navController.navigate(ROUTE_HIDDEN) },
                onBack = onFinish,
            )
        }
        composable(ROUTE_HIDDEN) {
            HiddenAppsScreen(onBack = { if (!navController.popBackStack()) onFinish() })
        }
    }
}

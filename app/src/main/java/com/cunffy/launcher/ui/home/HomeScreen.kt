package com.cunffy.launcher.ui.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.R
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.home.HomeEntry
import com.cunffy.launcher.data.home.HomeLayoutRepository.Companion.GRID_COLUMNS
import com.cunffy.launcher.data.home.HomeLayoutRepository.Companion.GRID_ROWS
import com.cunffy.launcher.security.BiometricAuthenticator
import com.cunffy.launcher.ui.components.Dock
import com.cunffy.launcher.ui.search.SearchPill
import com.cunffy.launcher.ui.settings.SettingsActivity
import com.cunffy.launcher.widgets.WidgetHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Home screen drawn over the wallpaper: clock + At-a-Glance, a drag-and-drop grid of app
 * shortcuts / folders / widgets (Room-backed), and a pinned search pill + dock. Long-pressing
 * the empty grid toggles edit mode.
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()
    val desktop by viewModel.desktop.collectAsStateWithLifecycle()
    val badgeCounts by viewModel.badgeCounts.collectAsStateWithLifecycle()
    val editMode by viewModel.editMode.collectAsStateWithLifecycle()

    val controller = remember { WidgetHostController(context) }
    var openFolder by remember { mutableStateOf<HomeEntry.Folder?>(null) }

    fun launchApp(app: AppInfo) {
        val activity = context as? FragmentActivity
        if (app.locked && activity != null) {
            BiometricAuthenticator.authenticate(
                activity,
                context.getString(R.string.unlock_app_title),
                context.getString(R.string.unlock_app_subtitle),
            ) { viewModel.launch(app) }
        } else {
            viewModel.launch(app)
        }
    }

    // System widget picker: allocates + binds an id, then we place it on the grid.
    val widgetPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (id != -1) {
                val info = controller.providerInfo(id)
                val spanX = ((info?.minWidth ?: 0) / WIDGET_CELL_PX + 1).coerceIn(1, GRID_COLUMNS)
                val spanY = ((info?.minHeight ?: 0) / WIDGET_CELL_PX + 1).coerceIn(1, GRID_ROWS)
                viewModel.addWidget(id, spanX, spanY, 0, 0)
            }
        }
    }

    fun pickWidget() {
        val id = controller.allocateId()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        widgetPicker.launch(intent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Clock(modifier = Modifier.padding(top = 36.dp))
        AtAGlance(modifier = Modifier.padding(top = 8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { viewModel.setEditMode(!editMode) })
                },
        ) {
            val density = LocalDensity.current
            val cellW = maxWidth / GRID_COLUMNS
            val cellH = maxHeight / GRID_ROWS
            val cellWpx = with(density) { cellW.toPx() }
            val cellHpx = with(density) { cellH.toPx() }

            desktop.forEach { entry ->
                key(entry.item.id) {
                    HomeItemView(
                        entry = entry,
                        cellW = cellW,
                        cellH = cellH,
                        cellWpx = cellWpx,
                        cellHpx = cellHpx,
                        editMode = editMode,
                        controller = controller,
                        badgeCount = (entry as? HomeEntry.App)
                            ?.let { badgeCounts[it.app.packageName] } ?: 0,
                        onLaunchApp = ::launchApp,
                        onOpenFolder = { openFolder = it },
                        onRemove = { viewModel.removeItem(entry) },
                        onDropped = { x, y -> handleDrop(desktop, entry, x, y, viewModel) },
                    )
                }
            }
        }

        if (editMode) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = ::pickWidget) { Text(stringResource(R.string.add_widget)) }
                Button(onClick = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(stringResource(R.string.settings_title)) }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Dock(apps = dockApps, onAppClick = ::launchApp)
            SearchPill(hint = stringResource(R.string.search_hint), onClick = onOpenDrawer)
        }
    }

    openFolder?.let { folder ->
        FolderDialog(
            folder = folder,
            onDismiss = { openFolder = null },
            onAppClick = { app -> openFolder = null; launchApp(app) },
        )
    }
}

private const val WIDGET_CELL_PX = 220

private fun handleDrop(
    desktop: List<HomeEntry>,
    entry: HomeEntry,
    cellX: Int,
    cellY: Int,
    viewModel: HomeViewModel,
) {
    val target = desktop.firstOrNull {
        it.item.id != entry.item.id && it.item.cellX == cellX && it.item.cellY == cellY
    }
    if (entry is HomeEntry.App && target is HomeEntry.App) {
        viewModel.mergeIntoFolder(entry.item, target.item)
    } else {
        viewModel.moveItem(entry.item, cellX, cellY)
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(10_000)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
        )
        Text(
            text = dateFormat.format(now),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

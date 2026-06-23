package com.cunffy.launcher.ui.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.cunffy.launcher.security.BiometricAuthenticator
import com.cunffy.launcher.ui.components.Dock
import com.cunffy.launcher.ui.drawer.AppActionsSheet
import com.cunffy.launcher.ui.drawer.AppEditDialog
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
@OptIn(ExperimentalLayoutApi::class)
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val controller = remember { WidgetHostController(context) }
    var openFolder by remember { mutableStateOf<HomeEntry.Folder?>(null) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var editApp by remember { mutableStateOf<AppInfo?>(null) }
    var confirmRemove by remember { mutableStateOf<HomeEntry?>(null) }

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

    // Pages: one per used page index, plus a trailing blank page while editing.
    val basePages = (desktop.maxOfOrNull { it.item.page } ?: 0) + 1
    val pageCount = if (editMode) basePages + 1 else basePages
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var pendingWidgetPage by remember { mutableStateOf(0) }

    // System widget picker: allocates + binds an id, then we place it on the current page.
    val widgetPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (id != -1) {
                val info = controller.providerInfo(id)
                val spanX = ((info?.minWidth ?: 0) / WIDGET_CELL_PX + 1)
                    .coerceIn(1, settings.gridColumns)
                val spanY = ((info?.minHeight ?: 0) / WIDGET_CELL_PX + 1)
                    .coerceIn(1, settings.gridRows)
                viewModel.addWidget(id, spanX, spanY, 0, 0, pendingWidgetPage)
            }
        }
    }

    fun pickWidget() {
        pendingWidgetPage = pagerState.currentPage
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
        Clock(use24h = settings.clock24h, modifier = Modifier.padding(top = 36.dp))
        AtAGlance(modifier = Modifier.padding(top = 8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            HomeGridPage(
                entries = desktop.filter { it.item.page == page },
                columns = settings.gridColumns,
                rows = settings.gridRows,
                editMode = editMode,
                controller = controller,
                badgeCounts = badgeCounts,
                onToggleEdit = { viewModel.setEditMode(!editMode) },
                onLaunchApp = ::launchApp,
                onLongClickApp = { menuApp = it },
                onOpenFolder = { openFolder = it },
                onRemove = { confirmRemove = it },
                onDropped = { entry, x, y -> handleDrop(desktop, entry, x, y, viewModel) },
                onCrossPage = { entry, delta, cellY ->
                    val newPage = (entry.item.page + delta).coerceAtLeast(0)
                    val newCellX = if (delta > 0) {
                        0
                    } else {
                        (settings.gridColumns - entry.item.spanX).coerceAtLeast(0)
                    }
                    viewModel.moveItemToPage(entry.item, newPage, newCellX, cellY)
                },
            )
        }

        PageIndicator(
            pageCount = pageCount,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(vertical = 6.dp),
        )

        if (editMode) {
            Text(
                text = "Editing — drag to move, ✕ to remove, drag past the edge for the next page",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Button(onClick = ::pickWidget) { Text(stringResource(R.string.add_widget)) }
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(stringResource(R.string.settings_title)) }
                Button(onClick = { viewModel.setEditMode(false) }) { Text("Done") }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MediaCard()
            Dock(
                apps = dockApps,
                onAppClick = ::launchApp,
                onAppLongClick = { menuApp = it },
                iconSize = settings.iconSizeDp.dp,
            )
        }
    }

    openFolder?.let { folder ->
        FolderDialog(
            folder = folder,
            onDismiss = { openFolder = null },
            onAppClick = { app -> openFolder = null; launchApp(app) },
            onRename = { title -> viewModel.renameFolder(folder.folder.id, title) },
        )
    }

    menuApp?.let { app ->
        AppActionsSheet(
            app = app,
            onDismiss = { menuApp = null },
            onInfo = { openAppInfo(context, app); menuApp = null },
            onEdit = { editApp = app; menuApp = null },
            onToggleHide = { viewModel.setHidden(app, !app.hidden); menuApp = null },
            onToggleLock = { viewModel.setLocked(app, !app.locked); menuApp = null },
            onAddToHome = { viewModel.addToHome(app); menuApp = null },
            onUninstall = { uninstall(context, app); menuApp = null },
            inDock = viewModel.isInDock(app),
            onToggleDock = {
                if (viewModel.isInDock(app)) viewModel.removeFromDock(app) else viewModel.addToDock(app)
                menuApp = null
            },
        )
    }

    editApp?.let { app ->
        AppEditDialog(
            app = app,
            onDismiss = { editApp = null },
            onSave = { label, category ->
                viewModel.setLabel(app, label)
                viewModel.setCategoryOverride(app, category.takeIf { it != app.category })
                editApp = null
            },
        )
    }

    confirmRemove?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove from home?") },
            text = {
                val name = (entry as? HomeEntry.App)?.app?.label
                    ?: (entry as? HomeEntry.Folder)?.folder?.title
                    ?: "this item"
                Text("Remove “$name” from the home screen? The app stays installed.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.removeItem(entry); confirmRemove = null }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("Cancel") }
            },
        )
    }
}

private fun openAppInfo(context: android.content.Context, app: AppInfo) {
    context.startActivity(
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun uninstall(context: android.content.Context, app: AppInfo) {
    context.startActivity(
        Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
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
        it.item.id != entry.item.id && it.item.page == entry.item.page &&
            it.item.cellX == cellX && it.item.cellY == cellY
    }
    if (entry is HomeEntry.App && target is HomeEntry.App) {
        viewModel.mergeIntoFolder(entry.item, target.item)
    } else {
        viewModel.moveItem(entry.item, cellX, cellY)
    }
}

@Composable
private fun HomeGridPage(
    entries: List<HomeEntry>,
    columns: Int,
    rows: Int,
    editMode: Boolean,
    controller: WidgetHostController,
    badgeCounts: Map<String, Int>,
    onToggleEdit: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongClickApp: (AppInfo) -> Unit,
    onOpenFolder: (HomeEntry.Folder) -> Unit,
    onRemove: (HomeEntry) -> Unit,
    onDropped: (HomeEntry, Int, Int) -> Unit,
    onCrossPage: (HomeEntry, Int, Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleEdit()
                })
            },
    ) {
        val density = LocalDensity.current
        val cellW = maxWidth / columns
        val cellH = maxHeight / rows
        val cellWpx = with(density) { cellW.toPx() }
        val cellHpx = with(density) { cellH.toPx() }

        entries.forEach { entry ->
            key(entry.item.id) {
                HomeItemView(
                    entry = entry,
                    cellW = cellW,
                    cellH = cellH,
                    cellWpx = cellWpx,
                    cellHpx = cellHpx,
                    columns = columns,
                    rows = rows,
                    editMode = editMode,
                    controller = controller,
                    badgeCount = (entry as? HomeEntry.App)
                        ?.let { badgeCounts[it.app.packageName] } ?: 0,
                    onLaunchApp = onLaunchApp,
                    onLongClickApp = onLongClickApp,
                    onOpenFolder = onOpenFolder,
                    onRemove = { onRemove(entry) },
                    onDropped = { x, y -> onDropped(entry, x, y) },
                    onCrossPage = { delta, cellY -> onCrossPage(entry, delta, cellY) },
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    if (pageCount <= 1) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) Color.White else Color.White.copy(alpha = 0.4f),
                    ),
            )
        }
    }
}

@Composable
private fun Clock(use24h: Boolean, modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(10_000)
        }
    }
    val timeFormat = remember(use24h) {
        SimpleDateFormat(if (use24h) "H:mm" else "h:mm", Locale.getDefault())
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

private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

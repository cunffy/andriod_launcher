package com.cunffy.launcher.ui.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cunffy.launcher.R
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.home.HomeEntry
import com.cunffy.launcher.security.BiometricAuthenticator
import com.cunffy.launcher.ui.components.Dock
import com.cunffy.launcher.ui.drawer.AppActionsSheet
import com.cunffy.launcher.ui.drawer.AppEditDialog
import com.cunffy.launcher.ui.settings.SettingsActivity
import com.cunffy.launcher.widgets.WidgetHostController
import com.cunffy.launcher.widgets.WidgetPickerSheet
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
    val pickerApps by viewModel.pickerApps.collectAsStateWithLifecycle()

    val controller = remember { WidgetHostController(context) }
    // The host must be listening for hosted widgets to receive their content — otherwise they
    // render blank/non-functional. Tie it to the lifecycle so it stops while we're backgrounded.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        controller.startListening()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> controller.startListening()
                Lifecycle.Event.ON_STOP -> controller.stopListening()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stopListening()
        }
    }
    var openFolder by remember { mutableStateOf<HomeEntry.Folder?>(null) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var editApp by remember { mutableStateOf<AppInfo?>(null) }
    var confirmRemove by remember { mutableStateOf<HomeEntry?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var confirmDeletePage by remember { mutableStateOf(false) }

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

    // Pages are an explicit, saved count (so added/empty pages persist), but never fewer
    // than the highest page an item actually sits on.
    val itemPages = (desktop.maxOfOrNull { it.item.page } ?: 0) + 1
    val pageCount = maxOf(settings.homePageCount, itemPages)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var pendingWidgetPage by remember { mutableStateOf(0) }
    // Page to follow once the pager has grown (after Add page / a cross-page move).
    var pendingScrollPage by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pageCount, pendingScrollPage) {
        pendingScrollPage?.let { target ->
            if (target in 0 until pageCount) {
                pagerState.animateScrollToPage(target)
                pendingScrollPage = null
            }
        }
    }

    // Custom widget picker: pick a provider, bind it (requesting permission / running its
    // configure activity if needed), then place it on the current page.
    var showWidgetPicker by remember { mutableStateOf(false) }
    var pendingWidgetId by remember { mutableStateOf(-1) }
    var pendingProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }

    fun placeWidget(id: Int) {
        val info = controller.providerInfo(id)
        val spanX = ((info?.minWidth ?: 0) / WIDGET_CELL_PX + 1).coerceIn(1, settings.gridColumns)
        val spanY = ((info?.minHeight ?: 0) / WIDGET_CELL_PX + 1).coerceIn(1, settings.gridRows)
        viewModel.addWidget(id, spanX, spanY, 0, 0, pendingWidgetPage)
        pendingWidgetId = -1
        pendingProvider = null
    }

    val widgetConfigureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId
        if (id != -1) {
            if (result.resultCode == Activity.RESULT_OK) placeWidget(id)
            else { controller.deleteId(id); pendingWidgetId = -1 }
        }
    }

    fun afterBound(id: Int, info: AppWidgetProviderInfo) {
        val configure = info.configure
        if (configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            val launched = runCatching { widgetConfigureLauncher.launch(intent); true }
                .getOrDefault(false)
            if (!launched) placeWidget(id)
        } else {
            placeWidget(id)
        }
    }

    val widgetBindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId
        val info = pendingProvider
        if (result.resultCode == Activity.RESULT_OK && id != -1 && info != null) {
            afterBound(id, info)
        } else if (id != -1) {
            controller.deleteId(id); pendingWidgetId = -1
        }
    }

    fun chooseWidget(info: AppWidgetProviderInfo) {
        showWidgetPicker = false
        pendingWidgetPage = pagerState.currentPage
        val id = controller.allocateId()
        pendingWidgetId = id
        pendingProvider = info
        if (controller.bindIfAllowed(id, info.provider)) {
            afterBound(id, info)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            val launched = runCatching { widgetBindLauncher.launch(intent); true }
                .getOrDefault(false)
            if (!launched) { controller.deleteId(id); pendingWidgetId = -1 }
        }
    }

    fun pickWidget() { showWidgetPicker = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Clock(
            use24h = settings.clock24h,
            sizeSp = settings.clockSizeSp,
            modifier = Modifier.padding(top = 36.dp),
        )
        if (settings.showAtAGlance) AtAGlance(modifier = Modifier.padding(top = 8.dp))

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
                showLabels = settings.showHomeLabels,
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
                    // Dragging past the last page creates (and persists) a new page.
                    if (newPage >= pageCount) viewModel.setHomePageCount(newPage + 1)
                    viewModel.moveItemToPage(entry.item, newPage, newCellX, cellY)
                    // Follow the item to its page once the pager reflects the move.
                    pendingScrollPage = newPage
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
                text = "Drag to move • ✕ to remove • drag past the edge to change page",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                ) {
                    EditAction(Icons.Rounded.Apps, "Add Icon") { showAppPicker = true }
                    EditAction(Icons.Rounded.AddCircleOutline, "Add page") {
                        // Add a real, persisted page and jump to it.
                        viewModel.setHomePageCount(pageCount + 1)
                        pendingScrollPage = pageCount
                    }
                    EditAction(Icons.Rounded.Widgets, "Widget", onClick = ::pickWidget)
                    EditAction(Icons.Rounded.Wallpaper, "Wallpaper") {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SET_WALLPAPER),
                                "Wallpaper",
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                    if (pageCount > 1) {
                        EditAction(Icons.Rounded.Delete, "Delete page") {
                            val onThisPage = desktop.count { it.item.page == pagerState.currentPage }
                            if (onThisPage == 0) {
                                val p = pagerState.currentPage
                                viewModel.deletePage(p)
                                pendingScrollPage = (p - 1).coerceAtLeast(0)
                            } else {
                                confirmDeletePage = true
                            }
                        }
                    }
                    EditAction(Icons.Rounded.Settings, "Settings") {
                        context.startActivity(
                            Intent(context, SettingsActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                    EditAction(Icons.Rounded.Check, "Done") { viewModel.setEditMode(false) }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.showMediaCard) MediaCard()
            Dock(
                apps = dockApps,
                onAppClick = ::launchApp,
                onAppLongClick = { menuApp = it },
                iconSize = settings.iconSizeDp.dp,
            )
        }
    }

    // Track the live folder from the desktop flow so removals/dissolves reflect immediately.
    val liveFolder = openFolder?.let { of ->
        desktop.filterIsInstance<HomeEntry.Folder>().firstOrNull { it.folder.id == of.folder.id }
    }
    LaunchedEffect(openFolder, liveFolder) {
        if (openFolder != null && liveFolder == null) openFolder = null
    }
    liveFolder?.let { folder ->
        FolderDialog(
            folder = folder,
            onDismiss = { openFolder = null },
            onAppClick = { app -> openFolder = null; launchApp(app) },
            onRename = { title -> viewModel.renameFolder(folder.folder.id, title) },
            onRemoveApp = { app -> viewModel.removeFromFolder(folder, app) },
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

    if (showWidgetPicker) {
        WidgetPickerSheet(
            controller = controller,
            onDismiss = { showWidgetPicker = false },
            onPick = { chooseWidget(it) },
        )
    }

    if (showAppPicker) {
        AppPickerSheet(
            apps = pickerApps,
            onDismiss = { showAppPicker = false },
            onPick = { viewModel.addToHome(it); showAppPicker = false },
        )
    }

    if (confirmDeletePage) {
        AlertDialog(
            onDismissRequest = { confirmDeletePage = false },
            title = { Text("Delete this page?") },
            text = {
                Text("Items on this page will be removed from the home screen. The apps stay installed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = pagerState.currentPage
                    viewModel.deletePage(p)
                    pendingScrollPage = (p - 1).coerceAtLeast(0)
                    confirmDeletePage = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletePage = false }) { Text("Cancel") }
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
    showLabels: Boolean,
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
                    showLabel = showLabels,
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

/** One labelled icon button in the edit toolbar (Add page, Widget, Wallpaper, …). */
@Composable
private fun EditAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 2.dp),
        )
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
private fun Clock(use24h: Boolean, sizeSp: Int = 64, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val now by produceState(initialValue = Date(), lifecycleOwner) {
        // Update aligned to the minute, and only while the launcher is on screen.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = Date()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
    val timeFormat = remember(use24h) {
        SimpleDateFormat(if (use24h) "H:mm" else "h:mm", Locale.getDefault())
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = sizeSp.sp,
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

package com.cunffy.launcher.ui.drawer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.R
import com.cunffy.launcher.data.apps.AppInfo
import com.cunffy.launcher.data.search.providers.WebSearchProvider
import com.cunffy.launcher.security.BiometricAuthenticator
import com.cunffy.launcher.ui.components.AppIcon
import com.cunffy.launcher.ui.search.SearchBar
import com.cunffy.launcher.ui.search.SearchResultsList
import com.cunffy.launcher.ui.search.SearchViewModel
import java.net.URLEncoder

/**
 * Slide-up app drawer: universal search on top, then either ranked search results (with app
 * hand-off chips) or the A–Z app grid with the category sidebar. Long-pressing an app opens
 * its actions sheet; locked apps require biometric auth before launching.
 */
@Composable
fun AppDrawerScreen(
    onRequestClose: () -> Unit,
    modifier: Modifier = Modifier,
    drawerViewModel: AppDrawerViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val apps by drawerViewModel.visibleApps.collectAsStateWithLifecycle()
    val categories by drawerViewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by drawerViewModel.selectedCategory.collectAsStateWithLifecycle()
    val settings by drawerViewModel.settings.collectAsStateWithLifecycle()

    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var editApp by remember { mutableStateOf<AppInfo?>(null) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    fun closeAndReset() {
        searchViewModel.clear()
        onRequestClose()
    }

    fun launchApp(app: AppInfo) {
        val activity = context as? FragmentActivity
        if (app.locked && activity != null) {
            BiometricAuthenticator.authenticate(
                activity,
                context.getString(R.string.unlock_app_title),
                context.getString(R.string.unlock_app_subtitle),
            ) { drawerViewModel.launch(app); closeAndReset() }
        } else {
            drawerViewModel.launch(app)
            closeAndReset()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 12.dp),
    ) {
        SearchBar(
            query = query,
            onQueryChange = searchViewModel::onQueryChange,
            onImeSearch = {
                if (query.isNotBlank()) {
                    WebSearchProvider.googleSearch(context, query)
                    closeAndReset()
                }
            },
            onClear = searchViewModel::clear,
            hint = stringResource(R.string.search_hint),
            autoFocus = settings.searchAutoFocus,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )

        if (query.isNotBlank()) {
            HandoffChips(query = query)
            SearchResultsList(
                results = results,
                onResultClick = { result ->
                    result.onActivate(context)
                    closeAndReset()
                },
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(apps, key = { it.key }) { app ->
                        AppIcon(
                            app = app,
                            onClick = { launchApp(app) },
                            onLongClick = { menuApp = app },
                            iconSize = settings.iconSizeDp.dp,
                            showLabel = settings.showDrawerLabels,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                AlphabetIndex(
                    apps = apps,
                    onLetter = { index -> scope.launch { gridState.scrollToItem(index) } },
                )
                CategorySidebar(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = drawerViewModel::selectCategory,
                    modifier = Modifier.width(56.dp).fillMaxHeight(),
                )
            }
        }
    }

    menuApp?.let { app ->
        AppActionsSheet(
            app = app,
            onDismiss = { menuApp = null },
            onInfo = { openAppInfo(context, app); menuApp = null },
            onEdit = { editApp = app; menuApp = null },
            onToggleHide = { drawerViewModel.setHidden(app, !app.hidden); menuApp = null },
            onToggleLock = { drawerViewModel.setLocked(app, !app.locked); menuApp = null },
            onAddToHome = { drawerViewModel.addToHome(app); menuApp = null },
            onUninstall = { uninstall(context, app); menuApp = null },
        )
    }

    editApp?.let { app ->
        AppEditDialog(
            app = app,
            onDismiss = { editApp = null },
            onSave = { label, category ->
                drawerViewModel.setLabel(app, label)
                drawerViewModel.setCategoryOverride(
                    app,
                    category.takeIf { it != app.category },
                )
                editApp = null
            },
        )
    }
}

/** Chips that continue the query inside Play Store / YouTube / Maps. */
@Composable
private fun HandoffChips(query: String) {
    val context = LocalContext.current
    val q = URLEncoder.encode(query, "UTF-8")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val targets = listOf(
            "Play Store" to "https://play.google.com/store/search?q=$q",
            "YouTube" to "https://www.youtube.com/results?search_query=$q",
            "Maps" to "https://www.google.com/maps/search/$q",
        )
        targets.forEach { (label, url) ->
            AssistChip(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

private fun openAppInfo(context: android.content.Context, app: AppInfo) {
    context.startActivity(
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun uninstall(context: android.content.Context, app: AppInfo) {
    context.startActivity(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** A–Z fast-scroll rail; tapping a letter jumps the grid to the first app under it. */
@Composable
private fun AlphabetIndex(apps: List<AppInfo>, onLetter: (Int) -> Unit) {
    if (apps.isEmpty()) return
    val letterToIndex = remember(apps) {
        val map = LinkedHashMap<Char, Int>()
        apps.forEachIndexed { index, app ->
            val first = app.label.firstOrNull()?.uppercaseChar()
            val letter = if (first != null && first.isLetter()) first else '#'
            if (letter !in map) map[letter] = index
        }
        map
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        letterToIndex.forEach { (letter, index) ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onLetter(index) }
                    .padding(vertical = 1.dp),
            )
        }
    }
}

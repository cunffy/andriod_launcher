package com.cunffy.launcher.ui.drawer

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunffy.launcher.R
import com.cunffy.launcher.data.search.providers.WebSearchProvider
import com.cunffy.launcher.ui.components.AppIcon
import com.cunffy.launcher.ui.search.SearchBar
import com.cunffy.launcher.ui.search.SearchResultsList
import com.cunffy.launcher.ui.search.SearchViewModel

/**
 * Slide-up app drawer: universal search on top, then either ranked search results or the
 * A–Z app grid with the category sidebar.
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

    fun closeAndReset() {
        searchViewModel.clear()
        onRequestClose()
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
                // Enter with nothing selected → Google search for the typed text.
                if (query.isNotBlank()) {
                    WebSearchProvider.googleSearch(context, query)
                    closeAndReset()
                }
            },
            onClear = searchViewModel::clear,
            hint = stringResource(R.string.search_hint),
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )

        if (query.isNotBlank()) {
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
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(apps, key = { it.key }) { app ->
                        AppIcon(
                            app = app,
                            onClick = {
                                drawerViewModel.launch(app)
                                closeAndReset()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                CategorySidebar(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = drawerViewModel::selectCategory,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

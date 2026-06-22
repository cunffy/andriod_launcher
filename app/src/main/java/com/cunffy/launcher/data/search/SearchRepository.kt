package com.cunffy.launcher.data.search

import com.cunffy.launcher.data.search.providers.AppSearchProvider
import com.cunffy.launcher.data.search.providers.FileSearchProvider
import com.cunffy.launcher.data.search.providers.SettingsSearchProvider
import com.cunffy.launcher.data.search.providers.WebSearchProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fans the query out to every [SearchProvider] in parallel and merges the results
 * into a single ranked list: grouped by [SearchResultType.rank] (apps, then settings,
 * then files, then the web fallback) and by descending relevance within each group.
 */
@Singleton
class SearchRepository @Inject constructor(
    apps: AppSearchProvider,
    settings: SettingsSearchProvider,
    files: FileSearchProvider,
    web: WebSearchProvider,
) {
    private val providers: List<SearchProvider> = listOf(apps, settings, files, web)

    suspend fun search(query: String): List<SearchResult> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
        providers
            .map { async { runCatching { it.query(query) }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }
            .sortedWith(compareBy<SearchResult> { it.type.rank }.thenByDescending { it.score })
    }
}

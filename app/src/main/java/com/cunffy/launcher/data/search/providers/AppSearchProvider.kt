package com.cunffy.launcher.data.search.providers

import com.cunffy.launcher.data.apps.AppCatalog
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import com.cunffy.launcher.data.search.matchScore
import javax.inject.Inject

/** Matches the query against installed app labels. Highest-priority provider. */
class AppSearchProvider @Inject constructor(
    private val appCatalog: AppCatalog,
) : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        return appCatalog.visibleApps.value
            .mapNotNull { app ->
                val score = matchScore(app.label, query)
                if (score == 0) return@mapNotNull null
                SearchResult(
                    id = "app:${app.key}",
                    title = app.label,
                    subtitle = null,
                    type = SearchResultType.APP,
                    icon = ResultIcon.OfDrawable(app.icon),
                    score = score,
                    onActivate = { appCatalog.launch(app) },
                )
            }
            .sortedByDescending { it.score }
            .take(MAX_RESULTS)
    }

    private companion object {
        const val MAX_RESULTS = 12
    }
}

package com.cunffy.launcher.data.search.providers

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Always-present fallback: a "Search Google for …" row. This is also the action fired
 * when the user presses Enter without selecting a result (see [googleSearch]).
 */
class WebSearchProvider @Inject constructor() : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return listOf(
            SearchResult(
                id = "web:$query",
                title = query,
                subtitle = "Search Google",
                type = SearchResultType.WEB,
                icon = ResultIcon.OfVector(Icons.Rounded.Search),
                score = 0,
                onActivate = { context -> googleSearch(context, query) },
            ),
        )
    }

    companion object {
        fun googleSearch(context: android.content.Context, query: String) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

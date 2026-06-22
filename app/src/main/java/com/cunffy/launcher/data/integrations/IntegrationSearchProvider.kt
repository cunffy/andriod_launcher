package com.cunffy.launcher.data.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import javax.inject.Inject

/**
 * Surfaces the built-in app integrations (Spotify, Slack, simPRO) in search: each offers to
 * search inside the app (or open it), deep-linking when installed and falling back to web.
 */
class IntegrationSearchProvider @Inject constructor() : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return Integration.ALL.map { integration ->
            val canSearch = integration.searchUri != null
            SearchResult(
                id = "integration:${integration.id}",
                title = if (canSearch) "Search ${integration.label} for “$query”"
                else "Open ${integration.label}",
                subtitle = integration.label,
                type = SearchResultType.INTEGRATION,
                icon = ResultIcon.OfVector(Icons.Rounded.OpenInNew),
                score = 30,
                onActivate = { context -> activate(context, integration, query) },
            )
        }
    }

    private fun activate(context: Context, integration: Integration, query: String) {
        val installed = isInstalled(context, integration.packageName)
        val intent = when {
            installed && integration.searchUriFor(query) != null ->
                Intent(Intent.ACTION_VIEW, Uri.parse(integration.searchUriFor(query)))
            installed ->
                context.packageManager.getLaunchIntentForPackage(integration.packageName)
                    ?: webIntent(integration.webFor(query))
            else -> webIntent(integration.webFor(query))
        }
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            context.startActivity(webIntent(integration.webFor(query)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun webIntent(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    private fun isInstalled(context: Context, packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0); true
    }.getOrDefault(false)
}

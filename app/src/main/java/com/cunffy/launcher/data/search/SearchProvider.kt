package com.cunffy.launcher.data.search

/**
 * A source of universal-search results (apps, settings, files, web …). Providers are
 * queried in parallel by [SearchRepository] and must be cheap/cancellable; return an
 * empty list when the query does not apply (e.g. permission missing).
 */
interface SearchProvider {
    suspend fun query(query: String): List<SearchResult>
}

/**
 * Shared relevance scoring used across providers so ranking is consistent.
 * Returns 0 when [text] does not match [query] at all.
 */
fun matchScore(text: String, query: String): Int {
    if (query.isBlank()) return 0
    val t = text.lowercase()
    val q = query.lowercase().trim()
    return when {
        t == q -> 100
        t.startsWith(q) -> 80
        t.split(' ', '.', '_', '-').any { it.startsWith(q) } -> 60
        t.contains(q) -> 40
        else -> 0
    }
}

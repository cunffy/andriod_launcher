package com.cunffy.launcher.data.integrations

/**
 * A first-class third-party app integration. The launcher can deep-link a search into the
 * app (when [searchUri] is set), open the app, or fall back to a web/store URL if it isn't
 * installed.
 */
data class Integration(
    val id: String,
    val label: String,
    val packageName: String,
    /** Deep-link template for searching inside the app; `%s` is replaced with the query. */
    val searchUri: String?,
    /** Web/store fallback template used when the app isn't installed; `%s` is the query. */
    val webFallback: String,
) {
    fun searchUriFor(query: String): String? = searchUri?.replace("%s", query)
    fun webFor(query: String): String = webFallback.replace("%s", query)

    companion object {
        val ALL = listOf(
            Integration(
                id = "spotify",
                label = "Spotify",
                packageName = "com.spotify.music",
                searchUri = "spotify:search:%s",
                webFallback = "https://open.spotify.com/search/%s",
            ),
            Integration(
                id = "slack",
                label = "Slack",
                packageName = "com.Slack",
                searchUri = null,
                webFallback = "https://app.slack.com/client",
            ),
            Integration(
                id = "simpro",
                label = "simPRO",
                packageName = "com.simprogroup.simpromobile",
                searchUri = null,
                webFallback = "https://play.google.com/store/apps/details?id=com.simprogroup.simpromobile",
            ),
        )
    }
}

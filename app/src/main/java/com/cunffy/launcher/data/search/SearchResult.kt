package com.cunffy.launcher.data.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.vector.ImageVector

/** Buckets used to group and order results in the UI (lower [rank] shows first). */
enum class SearchResultType(val rank: Int) {
    ANSWER(0),
    APP(1),
    CONTACT(2),
    SETTING(3),
    COMMAND(4),
    FILE(5),
    WEB(6),
}

/** An icon backed either by a loaded [Drawable] (apps/files) or a Compose [ImageVector]. */
sealed interface ResultIcon {
    data class OfDrawable(val drawable: Drawable) : ResultIcon
    data class OfVector(val image: ImageVector) : ResultIcon
}

/**
 * One row in the universal search results. [onActivate] performs the action
 * (launch app, open settings screen, view file, run web search).
 */
data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String?,
    val type: SearchResultType,
    val icon: ResultIcon,
    /** Higher = more relevant within the same [type]. */
    val score: Int,
    val onActivate: (Context) -> Unit,
)

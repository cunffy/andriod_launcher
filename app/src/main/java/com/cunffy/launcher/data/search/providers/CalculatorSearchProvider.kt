package com.cunffy.launcher.data.search.providers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import com.cunffy.launcher.data.search.Calculator
import com.cunffy.launcher.data.search.ResultIcon
import com.cunffy.launcher.data.search.SearchProvider
import com.cunffy.launcher.data.search.SearchResult
import com.cunffy.launcher.data.search.SearchResultType
import javax.inject.Inject

/** Inline calculator: evaluates an arithmetic query and offers to copy the result. */
class CalculatorSearchProvider @Inject constructor() : SearchProvider {

    override suspend fun query(query: String): List<SearchResult> {
        val result = Calculator.evaluate(query) ?: return emptyList()
        val formatted = format(result)
        return listOf(
            SearchResult(
                id = "calc:$query",
                title = "= $formatted",
                subtitle = "Tap to copy",
                type = SearchResultType.ANSWER,
                icon = ResultIcon.OfVector(Icons.Rounded.Calculate),
                score = 100,
                onActivate = { context -> copy(context, formatted) },
            ),
        )
    }

    private fun copy(context: Context, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("result", value))
        Toast.makeText(context, "Copied $value", Toast.LENGTH_SHORT).show()
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0 && !value.isInfinite()) value.toLong().toString()
        else value.toString()
}

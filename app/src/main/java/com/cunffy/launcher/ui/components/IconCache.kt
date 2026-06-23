package com.cunffy.launcher.ui.components

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Process-wide cache of rasterized app icons. Rasterizing a [Drawable] to a bitmap is the
 * expensive part of drawing an icon; doing it once per icon (and reusing across the drawer,
 * home, dock, and search) keeps scrolling smooth instead of re-rasterizing on every recompose.
 */
object IconCache {
    const val RENDER_PX = 160

    // ~512 icons; each ~100KB → bounded memory. Keyed by AppInfo.iconKey.
    private val cache = LruCache<String, ImageBitmap>(512)

    fun get(key: String, drawable: Drawable): ImageBitmap {
        cache.get(key)?.let { return it }
        val size = RENDER_PX
        val bitmap = runCatching { drawable.toBitmap(size, size) }
            .getOrElse { drawable.toBitmap() }
            .asImageBitmap()
        cache.put(key, bitmap)
        return bitmap
    }

    /** Pre-rasterize off the main thread so the first scroll is already warm. */
    fun warm(key: String, drawable: Drawable) {
        if (cache.get(key) == null) get(key, drawable)
    }
}

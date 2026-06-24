package com.cunffy.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * Two-level cache of rasterized app icons. Rasterizing (and, upstream, masking) a [Drawable]
 * is the expensive part of drawing an icon, and on a cold start the launcher would otherwise
 * re-render every icon before the home screen could paint — the few-second stall you see when
 * the process is killed in the background and relaunched.
 *
 *  - L1: a process-wide [LruCache] of [ImageBitmap]s (survives recompositions).
 *  - L2: a disk cache of PNGs keyed by [AppInfo.iconKey], so the masking/render cost is paid
 *    once ever and subsequent cold starts just decode a small file.
 */
object IconCache {
    const val RENDER_PX = 160

    // ~512 icons; each ~100KB → bounded memory. Keyed by AppInfo.iconKey.
    private val cache = LruCache<String, ImageBitmap>(512)

    private val ioExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var diskDir: File? = null

    /** Point the disk cache at a directory (call once, e.g. with `context.cacheDir`). */
    fun initialize(cacheRoot: File) {
        if (diskDir != null) return
        diskDir = File(cacheRoot, "icons").apply { runCatching { mkdirs() } }
    }

    /** True if a rendered icon for [key] is already on disk (lets callers skip re-masking it). */
    fun hasOnDisk(key: String): Boolean {
        if (cache.get(key) != null) return true
        val file = fileFor(key) ?: return false
        return file.exists()
    }

    fun get(key: String, drawable: Drawable): ImageBitmap {
        cache.get(key)?.let { return it }

        // L2: try the on-disk render before paying to rasterize the drawable again.
        readFromDisk(key)?.let {
            cache.put(key, it)
            return it
        }

        val size = RENDER_PX
        val bitmap = runCatching { drawable.toBitmap(size, size) }
            .getOrElse { drawable.toBitmap() }
        val image = bitmap.asImageBitmap()
        cache.put(key, image)
        persistToDisk(key, bitmap)
        return image
    }

    /** Pre-rasterize off the main thread so the first scroll is already warm. */
    fun warm(key: String, drawable: Drawable) {
        if (cache.get(key) == null) get(key, drawable)
    }

    private fun fileFor(key: String): File? {
        val dir = diskDir ?: return null
        return File(dir, hash(key) + ".png")
    }

    private fun readFromDisk(key: String): ImageBitmap? {
        val file = fileFor(key) ?: return null
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    private fun persistToDisk(key: String, bitmap: Bitmap) {
        val file = fileFor(key) ?: return
        ioExecutor.execute {
            runCatching {
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }.onFailure { runCatching { file.delete() } }
        }
    }

    private fun hash(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

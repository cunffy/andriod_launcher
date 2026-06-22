package com.cunffy.launcher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Minimal coroutine-friendly HTTP GET helper built on HttpURLConnection (no extra deps). */
object Http {

    suspend fun getString(url: String, timeoutMs: Int = 10_000): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                }
                connection.use { conn ->
                    if (conn.responseCode !in 200..299) return@runCatching null
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
            }.getOrNull()
        }

    /** Downloads [url] into [target], returning true on success. */
    suspend fun download(url: String, target: File, timeoutMs: Int = 30_000): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                }
                connection.use { conn ->
                    if (conn.responseCode !in 200..299) return@runCatching false
                    target.parentFile?.mkdirs()
                    conn.inputStream.use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                    true
                }
            }.getOrDefault(false)
        }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }
}

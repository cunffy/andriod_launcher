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
                try {
                    if (connection.responseCode in 200..299) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        null
                    }
                } finally {
                    connection.disconnect()
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
                try {
                    if (connection.responseCode !in 200..299) {
                        false
                    } else {
                        target.parentFile?.mkdirs()
                        connection.inputStream.use { input ->
                            FileOutputStream(target).use { output -> input.copyTo(output) }
                        }
                        true
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(false)
        }
}

package com.cunffy.launcher.data.theme

import android.app.WallpaperManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the primary color the wallpaper reports via [WallpaperManager.getWallpaperColors]. This
 * works for *live* wallpapers too (they publish colors through onComputeColors), which is what
 * lets us offer wallpaper-based theming on ROMs like RedMagic where the system Material You
 * picker ignores live wallpapers. No permission is required to read colors.
 */
@Singleton
class WallpaperColorProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wallpaperManager = WallpaperManager.getInstance(context)

    private val _primaryColor = MutableStateFlow(readPrimary())
    val primaryColor: StateFlow<Int?> = _primaryColor.asStateFlow()

    init {
        runCatching {
            wallpaperManager.addOnColorsChangedListener(
                { _, which ->
                    if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                        _primaryColor.value = readPrimary()
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
    }

    /** Current wallpaper primary color as ARGB, or null if unavailable. */
    fun current(): Int? = _primaryColor.value ?: readPrimary().also { _primaryColor.value = it }

    private fun readPrimary(): Int? = runCatching {
        wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
    }.getOrNull()
}

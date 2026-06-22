package com.cunffy.launcher.data.icons

import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces the final icon for an app, applying (in priority order): a selected icon pack,
 * then wallpaper-themed monochrome styling, else the stock icon. Failures fall back to the
 * stock icon so a bad pack never blanks the drawer.
 */
@Singleton
class IconResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val iconPackRepository: IconPackRepository,
) {
    fun resolve(
        component: ComponentName,
        baseIcon: Drawable,
        iconPackPackage: String?,
        themed: Boolean,
    ): Drawable {
        if (iconPackPackage != null && iconPackRepository.isInstalled(iconPackPackage)) {
            iconPackRepository.pack(iconPackPackage).getIcon(component)?.let { return it }
        }
        if (themed) {
            themedMonochrome(baseIcon)?.let { return it }
        }
        return baseIcon
    }

    /**
     * Builds a Material You themed icon from the adaptive icon's monochrome layer (API 33+):
     * the system accent tints the glyph over a soft accent background.
     */
    private fun themedMonochrome(base: Drawable): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        if (base !is AdaptiveIconDrawable) return null
        val mono = base.monochrome ?: return null

        val size = 192
        val res = context.resources
        val bgColor = res.getColor(android.R.color.system_accent1_100, context.theme)
        val fgColor = res.getColor(android.R.color.system_accent1_700, context.theme)

        val bitmap = android.graphics.Bitmap.createBitmap(
            size, size, android.graphics.Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)
        mono.mutate()
        mono.setBounds(0, 0, size, size)
        mono.setTint(fgColor)
        mono.setTintMode(PorterDuff.Mode.SRC_IN)
        mono.draw(canvas)
        return BitmapDrawable(res, bitmap)
    }
}

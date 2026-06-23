package com.cunffy.launcher.data.icons

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces the final icon for an app, applying (in priority order): a selected icon pack,
 * then wallpaper-themed monochrome styling, else the stock icon normalized to a uniform
 * circle — the cohesive Pixel-launcher look. Failures fall back to the stock icon so a bad
 * pack never blanks the drawer.
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
            // Icon packs ship their own shape; pass them through untouched.
            iconPackRepository.pack(iconPackPackage).getIcon(component)?.let { return it }
        }
        if (themed) {
            themedMonochrome(baseIcon)?.let { return it }
        }
        return runCatching { circular(baseIcon) }.getOrDefault(baseIcon)
    }

    /**
     * Renders any icon into a uniform circle, like the Pixel launcher: adaptive icons are
     * masked to the circle; legacy icons are centered on a white circle so they match.
     */
    private fun circular(base: Drawable): Drawable {
        val src = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(src)
        if (base is AdaptiveIconDrawable) {
            base.setBounds(0, 0, SIZE, SIZE)
            base.draw(srcCanvas)
        } else {
            // Legacy icon: white backing + inset glyph so it fills the circle like adaptive ones.
            srcCanvas.drawColor(Color.WHITE)
            val inset = (SIZE * LEGACY_INSET).toInt()
            base.setBounds(inset, inset, SIZE - inset, SIZE - inset)
            base.draw(srcCanvas)
        }
        return BitmapDrawable(context.resources, maskToCircle(src))
    }

    /** Antialiased circular crop of [src]. */
    private fun maskToCircle(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /**
     * Builds a Material You themed icon from the adaptive icon's monochrome layer (API 33+):
     * the system accent tints the glyph over a soft accent background, cropped to a circle.
     */
    private fun themedMonochrome(base: Drawable): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        if (base !is AdaptiveIconDrawable) return null
        val mono = base.monochrome ?: return null

        val res = context.resources
        val bgColor = res.getColor(android.R.color.system_accent1_100, context.theme)
        val fgColor = res.getColor(android.R.color.system_accent1_700, context.theme)

        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)
        mono.mutate()
        mono.setBounds(0, 0, SIZE, SIZE)
        mono.setTint(fgColor)
        mono.setTintMode(PorterDuff.Mode.SRC_IN)
        mono.draw(canvas)
        return BitmapDrawable(res, maskToCircle(bitmap))
    }

    private companion object {
        const val SIZE = 192
        const val LEGACY_INSET = 0.16f
    }
}

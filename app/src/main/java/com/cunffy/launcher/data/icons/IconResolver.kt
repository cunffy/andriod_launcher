package com.cunffy.launcher.data.icons

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.cunffy.launcher.data.prefs.IconShape
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces the final icon for an app, applying (in priority order): a selected icon pack,
 * then wallpaper-themed monochrome styling, else the stock icon — normalized to the user's
 * chosen [IconShape] (circle, squircle, rounded square, square) for a cohesive look.
 * Failures fall back to the stock icon so a bad pack never blanks the drawer.
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
        shape: IconShape,
    ): Drawable {
        if (iconPackPackage != null && iconPackRepository.isInstalled(iconPackPackage)) {
            val pack = iconPackRepository.pack(iconPackPackage)
            // Icon packs ship their own shape; pass them through untouched.
            pack.getIcon(component)?.let { return it }
            // No explicit icon: style the stock icon with the pack's back/mask so it still
            // matches the pack instead of standing out as a plain system icon.
            pack.maskSystemIcon(baseIcon, component)?.let { return it }
        }
        if (themed) {
            themedMonochrome(baseIcon, shape)?.let { return it }
        }
        return runCatching { shaped(baseIcon, shape) }.getOrDefault(baseIcon)
    }

    /**
     * Renders any icon into the chosen [shape], like the Pixel launcher: adaptive icons are
     * masked to the shape; legacy icons are centered on a white backing so they match.
     */
    private fun shaped(base: Drawable, shape: IconShape): Drawable {
        val src = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(src)
        // Both adaptive and legacy icons are scaled to fill the tile, then cropped to the
        // shape. Filling (rather than insetting legacy icons on a white circle) keeps full-bleed
        // OEM icons like the camera looking clean instead of shrunken on a white background.
        base.setBounds(0, 0, SIZE, SIZE)
        base.draw(srcCanvas)
        return BitmapDrawable(context.resources, maskToShape(src, shape))
    }

    /** Antialiased crop of [src] to [shape]. */
    private fun maskToShape(src: Bitmap, shape: IconShape): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(shapePath(shape), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun shapePath(shape: IconShape): Path {
        val s = SIZE.toFloat()
        val path = Path()
        when (shape) {
            IconShape.CIRCLE -> path.addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW)
            IconShape.SQUIRCLE ->
                path.addRoundRect(RectF(0f, 0f, s, s), s * 0.42f, s * 0.42f, Path.Direction.CW)
            IconShape.ROUNDED_SQUARE ->
                path.addRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, Path.Direction.CW)
            IconShape.SQUARE ->
                path.addRoundRect(RectF(0f, 0f, s, s), s * 0.08f, s * 0.08f, Path.Direction.CW)
        }
        return path
    }

    /**
     * Builds a Material You themed icon from the adaptive icon's monochrome layer (API 33+):
     * the system accent tints the glyph over a soft accent background, cropped to [shape].
     */
    private fun themedMonochrome(base: Drawable, shape: IconShape): Drawable? {
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
        return BitmapDrawable(res, maskToShape(bitmap, shape))
    }

    private companion object {
        const val SIZE = 192
    }
}

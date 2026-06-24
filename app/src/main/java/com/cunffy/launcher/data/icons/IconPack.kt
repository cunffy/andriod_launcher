package com.cunffy.launcher.data.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs
import org.xmlpull.v1.XmlPullParser

/** Metadata for an installed third-party icon pack. */
data class IconPackInfo(
    val packageName: String,
    val label: String,
)

/**
 * Lazily parses an icon pack's `appfilter.xml` (component → drawable-name map) and resolves
 * themed drawables from the pack's resources. All lookups are best-effort and return null on
 * any failure so the caller can fall back to the system icon.
 */
class IconPack(
    private val context: Context,
    val packageName: String,
) {
    private val componentToDrawable = HashMap<String, String>()
    private val iconBacks = ArrayList<String>()
    private var iconMask: String? = null
    private var iconUpon: String? = null
    private var iconScale = 1f
    private var packResources: Resources? = null
    private var loaded = false

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(packageName)
            packResources = res
            val parser = openAppFilter(pm, res) ?: return
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            val component = parser.getAttributeValue(null, "component")
                            val drawable = parser.getAttributeValue(null, "drawable")
                            if (component != null && drawable != null) {
                                normalizeComponent(component)?.let {
                                    componentToDrawable[it] = drawable
                                }
                            }
                        }
                        // Pack-wide styling applied to apps the pack doesn't theme explicitly,
                        // so the whole drawer matches the pack instead of showing stock icons.
                        "iconback" -> collectImgs(parser).let { iconBacks.addAll(it) }
                        "iconmask" -> collectImgs(parser).firstOrNull()?.let { iconMask = it }
                        "iconupon" -> collectImgs(parser).firstOrNull()?.let { iconUpon = it }
                        "scale" -> parser.getAttributeValue(null, "factor")
                            ?.toFloatOrNull()?.let { iconScale = it }
                    }
                }
                event = parser.next()
            }
        }
    }

    /** Reads img1, img2, … attributes off the current tag (packs ship 1..n background variants). */
    private fun collectImgs(parser: XmlPullParser): List<String> {
        val imgs = ArrayList<String>()
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i).startsWith("img")) {
                parser.getAttributeValue(i)?.takeIf { it.isNotBlank() }?.let { imgs.add(it) }
            }
        }
        return imgs
    }

    private fun openAppFilter(pm: PackageManager, res: Resources): XmlPullParser? {
        // Most packs ship res/xml/appfilter.xml; some ship assets/appfilter.xml.
        val xmlId = res.getIdentifier("appfilter", "xml", packageName)
        if (xmlId != 0) return res.getXml(xmlId)
        return runCatching {
            val factory = android.util.Xml.newPullParser()
            factory.setInput(res.assets.open("appfilter.xml"), "utf-8")
            factory
        }.getOrNull()
    }

    /** "ComponentInfo{pkg/cls}" or "pkg/cls" → flattened "pkg/cls". */
    private fun normalizeComponent(raw: String): String? {
        val inner = raw.removePrefix("ComponentInfo{").removeSuffix("}")
        return if (inner.contains('/')) inner else null
    }

    fun getIcon(component: ComponentName): Drawable? {
        ensureLoaded()
        val res = packResources ?: return null
        val drawableName = componentToDrawable[component.flattenToShortString()]
            ?: componentToDrawable["${component.packageName}/${component.className}"]
            ?: return null
        val id = res.getIdentifier(drawableName, "drawable", packageName)
        if (id == 0) return null
        return runCatching {
            @Suppress("DEPRECATION")
            res.getDrawable(id, context.theme)
        }.getOrNull()
    }

    /**
     * Styles a stock app icon with the pack's back/mask/upon layers and scale, so apps the pack
     * has no explicit icon for still look like they belong to the pack. Returns null when the
     * pack defines no such styling (caller then keeps the system icon).
     */
    fun maskSystemIcon(base: Drawable, component: ComponentName): Drawable? {
        ensureLoaded()
        val res = packResources ?: return null
        if (iconBacks.isEmpty() && iconMask == null && iconUpon == null && iconScale == 1f) {
            return null
        }
        return runCatching {
            val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)

            pickBack(component)?.let { loadPackDrawable(it) }?.let {
                it.setBounds(0, 0, SIZE, SIZE)
                it.draw(canvas)
            }

            val scaled = (SIZE * iconScale).toInt().coerceIn(1, SIZE)
            val inset = (SIZE - scaled) / 2f
            val baseBitmap = base.toBitmap(scaled, scaled)

            val maskBitmap = iconMask?.let { loadPackDrawable(it) }?.toBitmap(SIZE, SIZE)
            if (maskBitmap != null) {
                val layer = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
                val layerCanvas = Canvas(layer)
                layerCanvas.drawBitmap(baseBitmap, inset, inset, null)
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                layerCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
                canvas.drawBitmap(layer, 0f, 0f, null)
            } else {
                canvas.drawBitmap(baseBitmap, inset, inset, null)
            }

            iconUpon?.let { loadPackDrawable(it) }?.let {
                it.setBounds(0, 0, SIZE, SIZE)
                it.draw(canvas)
            }
            BitmapDrawable(res, out)
        }.getOrNull()
    }

    private fun pickBack(component: ComponentName): String? {
        if (iconBacks.isEmpty()) return null
        // Deterministic per app so an icon doesn't change background between launches.
        return iconBacks[abs(component.flattenToShortString().hashCode()) % iconBacks.size]
    }

    private fun loadPackDrawable(name: String): Drawable? {
        val res = packResources ?: return null
        val id = res.getIdentifier(name, "drawable", packageName)
        if (id == 0) return null
        return runCatching {
            @Suppress("DEPRECATION")
            res.getDrawable(id, context.theme)
        }.getOrNull()
    }

    private companion object {
        const val SIZE = 192
    }
}

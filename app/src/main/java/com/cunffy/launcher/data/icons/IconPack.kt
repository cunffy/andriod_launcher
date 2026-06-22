package com.cunffy.launcher.data.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
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
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        normalizeComponent(component)?.let { componentToDrawable[it] = drawable }
                    }
                }
                event = parser.next()
            }
        }
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
}

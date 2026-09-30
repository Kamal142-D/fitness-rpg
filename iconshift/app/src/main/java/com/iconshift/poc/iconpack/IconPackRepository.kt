package com.iconshift.poc.iconpack

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.LruCache
import android.util.Xml
import androidx.core.graphics.drawable.toBitmap
import com.iconshift.core.applyengine.IconSource
import com.iconshift.core.iconpack.IconPackIndex
import com.iconshift.core.iconpack.IconPackIndexBuilder
import com.iconshift.core.iconpack.IconPackInfo
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class IconPackReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Discovers icon packs installed on the phone and reads their icons in place. Nothing is copied
 * or bundled: drawables are loaded from the pack's own resources on demand.
 */
class IconPackRepository(private val context: Context) {

    private val pm = context.packageManager
    private val resources = ConcurrentHashMap<String, Resources>()
    private val indexes = ConcurrentHashMap<String, IconPackIndex>()
    private val resIds = ConcurrentHashMap<String, Map<String, Int>>()

    private val thumbnails = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    /** Packs advertising any of the common launcher icon-pack intents, sorted by name. */
    fun detectPacks(): List<IconPackInfo> {
        val intents = PACK_ACTIONS.map { Intent(it) } +
            PACK_CATEGORIES.map { Intent(Intent.ACTION_MAIN).addCategory(it) }
        val packages = LinkedHashSet<String>()
        for (intent in intents) {
            runCatching { pm.queryIntentActivities(intent, 0) }.getOrNull()
                ?.forEach { packages += it.activityInfo.packageName }
        }
        packages -= context.packageName
        return packages.map { pkg ->
            IconPackInfo(pkg, label(pkg), indexes[pkg]?.entries?.size)
        }.sortedBy { it.label.lowercase() }
    }

    fun label(pkg: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    fun appIcon(pkg: String, sizePx: Int = 96): Bitmap? = runCatching {
        pm.getApplicationIcon(pkg).toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    fun cachedIndex(pkg: String): IconPackIndex? = indexes[pkg]

    /** Parses appfilter.xml + drawable.xml once per pack. Throws [IconPackReadException]. */
    fun loadIndex(pkg: String): IconPackIndex {
        indexes[pkg]?.let { return it }
        val res = resourcesFor(pkg)
        val builder = IconPackIndexBuilder(pkg)
        val hasAppFilter = readXml(pkg, res, "appfilter") { p ->
            builder.onAppFilterItem(p.getAttributeValue(null, "component"), p.getAttributeValue(null, "drawable"))
        }
        val hasDrawables = readXml(pkg, res, "drawable") { p ->
            builder.onDrawableItem(p.getAttributeValue(null, "drawable"))
        }
        if (!hasAppFilter && !hasDrawables) {
            throw IconPackReadException("This icon pack could not be read (no appfilter.xml or drawable.xml).")
        }
        val ids = HashMap<String, Int>()
        val index = builder.build { name ->
            @Suppress("DiscouragedApi")
            val id = res.getIdentifier(name, "drawable", pkg)
            if (id != 0) ids[name] = id
            id != 0
        }
        if (index.entries.isEmpty()) throw IconPackReadException("This icon pack could not be read (no usable icons).")
        resIds[pkg] = ids
        indexes[pkg] = index
        return index
    }

    /** Small, cached preview. Decoded lazily per grid cell, never for the whole pack. */
    fun thumbnail(pkg: String, drawableName: String, sizePx: Int): Bitmap? {
        val key = "$pkg/$drawableName@$sizePx"
        thumbnails.get(key)?.let { return it }
        val bmp = render(pkg, drawableName, sizePx, DisplayMetrics.DENSITY_XHIGH) ?: return null
        thumbnails.put(key, bmp)
        return bmp
    }

    /** Full-size PNG of the chosen icon, ready for the apply engines. */
    fun iconSource(pkg: String, drawableName: String): IconSource {
        val bmp = render(pkg, drawableName, ICON_SIZE, DisplayMetrics.DENSITY_XXXHIGH)
            ?: throw IconPackReadException("This icon is no longer available.")
        val png = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        return IconSource(png, "${label(pkg)} · $drawableName", "pack:$pkg/$drawableName")
    }

    private fun render(pkg: String, drawableName: String, sizePx: Int, density: Int): Bitmap? = runCatching {
        val res = resourcesFor(pkg)
        @Suppress("DiscouragedApi")
        val id = resIds[pkg]?.get(drawableName) ?: res.getIdentifier(drawableName, "drawable", pkg)
        if (id == 0) return null
        res.getDrawableForDensity(id, density, null)?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    private fun resourcesFor(pkg: String): Resources = resources.getOrPut(pkg) {
        try {
            pm.getResourcesForApplication(pkg)
        } catch (e: Exception) {
            throw IconPackReadException("The icon pack was uninstalled or could not be opened.", e)
        }
    }

    /** Reads `<item>` tags from res/xml/<name>.xml, falling back to assets/<name>.xml. False if neither exists. */
    private fun readXml(pkg: String, res: Resources, name: String, onItem: (XmlPullParser) -> Unit): Boolean {
        @Suppress("DiscouragedApi")
        val xmlId = res.getIdentifier(name, "xml", pkg)
        var stream: InputStream? = null
        val parser: XmlPullParser = if (xmlId != 0) {
            res.getXml(xmlId)
        } else {
            val asset = runCatching { context.createPackageContext(pkg, 0).assets.open("$name.xml") }.getOrNull()
                ?: return false
            stream = asset
            Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(asset, null)
            }
        }
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") onItem(parser)
                event = parser.next()
            }
        } catch (e: Exception) {
            // A malformed tail still leaves the items read so far usable.
        } finally {
            (parser as? XmlResourceParser)?.close()
            stream?.close()
        }
        return true
    }

    companion object {
        const val ICON_SIZE = 432

        private val PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.dlto.atom.launcher.THEME",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
        )
        private val PACK_CATEGORIES = listOf(
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
        )
    }
}

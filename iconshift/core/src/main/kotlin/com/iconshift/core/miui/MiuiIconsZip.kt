package com.iconshift.core.miui

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Reads and rewrites the MIUI/HyperOS theme `icons` component: a zip whose per-app icons live at
 * `res/drawable-<density>/<package>.png`. Some themes also ship per-app folders (layered or
 * "fancy" icons such as `fancy_icons/<package>/...` or `res/drawable-xxhdpi/<package>/0.png`)
 * that take priority over the flat PNG, so every entry belonging to a package is replaced together.
 *
 * Everything not belonging to an overridden package is copied through untouched, so other apps
 * keep their current themed icons.
 */
object MiuiIconsZip {

    const val DEFAULT_ICON_DIR = "res/drawable-xxhdpi"

    private val ICON_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg", "xml")

    fun entryName(packageName: String, iconDir: String = DEFAULT_ICON_DIR): String =
        "$iconDir/$packageName.png"

    /** Reads all entries (name -> bytes), preserving order. Directory entries are skipped. */
    fun read(zip: ByteArray): LinkedHashMap<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zip)).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                if (!e.isDirectory) out[e.name] = zin.readBytes()
                zin.closeEntry()
            }
        }
        return out
    }

    fun write(entries: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zout ->
            for ((name, bytes) in entries) {
                zout.putNextEntry(ZipEntry(name))
                zout.write(bytes)
                zout.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    /** True if [entryPath] is one of [packageName]'s icon entries (flat, activity-specific, or in a per-app folder). */
    fun belongsTo(entryPath: String, packageName: String): Boolean {
        val segments = entryPath.split('/')
        val file = segments.last()
        // Package names contain dots, so only strip a known file extension.
        val ext = file.substringAfterLast('.', "").lowercase()
        val stem = if (ext in ICON_EXTENSIONS) file.substringBeforeLast('.') else file
        if (stem == packageName || stem.startsWith("$packageName#")) return true
        return segments.dropLast(1).any { it == packageName }
    }

    fun entriesFor(entries: Map<String, ByteArray>, packageName: String): Map<String, ByteArray> =
        entries.filterKeys { belongsTo(it, packageName) }

    /**
     * Picks the folder where most flat per-app icons live (e.g. `res/drawable-xxhdpi`), so overrides
     * land next to the theme's own icons. Falls back to [DEFAULT_ICON_DIR].
     */
    fun detectIconDir(entries: Map<String, ByteArray>): String =
        entries.keys
            .filter { it.startsWith("res/drawable") && it.endsWith(".png") && it.count { c -> c == '/' } == 2 }
            .groupingBy { it.substringBeforeLast('/') }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: DEFAULT_ICON_DIR

    /**
     * Returns a new icons zip: every entry of [dropPackages] removed, then [put] added.
     * [base] may be null when no icons component is currently applied.
     */
    fun rewrite(
        base: ByteArray?,
        dropPackages: Set<String>,
        put: Map<String, ByteArray>,
    ): ByteArray {
        val entries = if (base != null) read(base) else LinkedHashMap()
        val kept = LinkedHashMap<String, ByteArray>()
        for ((name, bytes) in entries) {
            if (dropPackages.none { belongsTo(name, it) }) kept[name] = bytes
        }
        kept.putAll(put)
        return write(kept)
    }
}

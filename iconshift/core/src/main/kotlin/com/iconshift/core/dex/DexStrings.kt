package com.iconshift.core.dex

/**
 * Reads the string table of a DEX file (used to learn which intent extras and keys ThemeManager's
 * apply screen expects, without decompiling or root). Only the header, `string_ids` and
 * `string_data_item`s are touched.
 */
object DexStrings {

    private const val HEADER_SIZE = 0x70
    private const val STRING_IDS_SIZE_OFF = 0x38
    private const val STRING_IDS_OFF_OFF = 0x3C

    fun isDex(bytes: ByteArray): Boolean =
        bytes.size >= HEADER_SIZE && bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() &&
            bytes[2] == 'x'.code.toByte() && bytes[3] == '\n'.code.toByte()

    /** All strings in the table, in DEX order. Throws [IllegalArgumentException] on malformed input. */
    fun read(dex: ByteArray): List<String> {
        require(isDex(dex)) { "not a DEX file" }
        val count = u32(dex, STRING_IDS_SIZE_OFF)
        val idsOff = u32(dex, STRING_IDS_OFF_OFF)
        require(count >= 0 && idsOff >= 0 && idsOff.toLong() + count.toLong() * 4 <= dex.size) { "bad string_ids" }
        return List(count) { i -> decodeAt(dex, u32(dex, idsOff + i * 4)) }
    }

    private val KEYWORDS = listOf(
        "theme_file", "api_called", "file_path", "screenshot", "applytheme", "apply_theme",
        "mtz", "/data/system/theme", ".runtime", "theme_magic", "designer", "local_theme",
        "import_theme", "icons",
    )

    /** Strings that look related to applying a local theme: extras, keys, paths, log messages. */
    fun interesting(strings: Iterable<String>, limit: Int = 200): List<String> =
        strings.asSequence()
            .filter { it.length in 3..200 }
            .filter { s -> val l = s.lowercase(); KEYWORDS.any { l.contains(it) } }
            .distinct()
            .take(limit)
            .toList()

    private fun decodeAt(dex: ByteArray, offset: Int): String {
        require(offset in 0 until dex.size) { "bad string offset $offset" }
        // uleb128 utf16 length (not needed for decoding), then MUTF-8 bytes up to a 0 terminator.
        var p = offset
        while (p < dex.size && dex[p].toInt() and 0x80 != 0) p++
        p++
        val sb = StringBuilder()
        while (p < dex.size) {
            val a = dex[p].toInt() and 0xFF
            if (a == 0) break
            when {
                a < 0x80 -> { sb.append(a.toChar()); p += 1 }
                a and 0xE0 == 0xC0 && p + 1 < dex.size -> {
                    val b = dex[p + 1].toInt() and 0x3F
                    sb.append((((a and 0x1F) shl 6) or b).toChar()); p += 2
                }
                a and 0xF0 == 0xE0 && p + 2 < dex.size -> {
                    val b = dex[p + 1].toInt() and 0x3F
                    val c = dex[p + 2].toInt() and 0x3F
                    sb.append((((a and 0x0F) shl 12) or (b shl 6) or c).toChar()); p += 3
                }
                else -> { sb.append('�'); p += 1 }
            }
        }
        return sb.toString()
    }

    private fun u32(b: ByteArray, off: Int): Int {
        require(off + 4 <= b.size) { "truncated DEX" }
        return (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)
    }
}

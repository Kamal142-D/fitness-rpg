package com.iconshift.core.dex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayOutputStream

class DexStringsTest {

    /** Minimal DEX: 0x70-byte header with string_ids pointing at MUTF-8 string_data_items. */
    private fun fakeDex(strings: List<String>): ByteArray {
        val header = ByteArray(0x70)
        "dex\n035\u0000".forEachIndexed { i, c -> header[i] = c.code.toByte() }
        val idsOff = 0x70
        val dataStart = idsOff + strings.size * 4
        val data = ByteArrayOutputStream()
        val offsets = strings.map { s ->
            val off = dataStart + data.size()
            data.write(s.length) // uleb128 for short strings
            data.write(mutf8(s))
            data.write(0)
            off
        }
        fun putU32(buf: ByteArray, at: Int, v: Int) { for (k in 0..3) buf[at + k] = (v ushr (8 * k)).toByte() }
        putU32(header, 0x38, strings.size)
        putU32(header, 0x3C, idsOff)
        val ids = ByteArray(strings.size * 4).also { b -> offsets.forEachIndexed { i, o -> putU32(b, i * 4, o) } }
        return header + ids + data.toByteArray()
    }

    private fun mutf8(s: String): ByteArray {
        val out = ByteArrayOutputStream()
        for (ch in s) {
            val c = ch.code
            when {
                c in 1..0x7F -> out.write(c)
                c <= 0x7FF -> { out.write(0xC0 or (c shr 6)); out.write(0x80 or (c and 0x3F)) }
                else -> { out.write(0xE0 or (c shr 12)); out.write(0x80 or ((c shr 6) and 0x3F)); out.write(0x80 or (c and 0x3F)) }
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `reads ascii and multi-byte strings in order`() {
        val strings = listOf("theme_file_path", "api_called_from", "Lcom/android/thememanager/ApplyThemeForScreenshot;", "é", "中")
        assertEquals(strings, DexStrings.read(fakeDex(strings)))
    }

    @Test
    fun `interesting keeps theme apply related strings only`() {
        val strings = listOf("theme_file_path", "hello", "api_called_from", "/data/system/theme/.runtime", "ok", "theme_file_path")
        assertEquals(
            listOf("theme_file_path", "api_called_from", "/data/system/theme/.runtime"),
            DexStrings.interesting(DexStrings.read(fakeDex(strings))),
        )
    }

    @Test
    fun `rejects non-dex input`() {
        assertFalse(DexStrings.isDex(ByteArray(10)))
    }
}

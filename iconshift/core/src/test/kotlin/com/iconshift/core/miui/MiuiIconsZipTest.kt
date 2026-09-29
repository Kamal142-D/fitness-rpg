package com.iconshift.core.miui

import com.iconshift.core.fakePng
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuiIconsZipTest {

    @Test
    fun `belongsTo matches flat, activity-specific and per-app folder entries`() {
        assertTrue(MiuiIconsZip.belongsTo("res/drawable-xxhdpi/com.whatsapp.png", "com.whatsapp"))
        assertTrue(MiuiIconsZip.belongsTo("res/drawable-xxhdpi/com.whatsapp#com.whatsapp.Main.png", "com.whatsapp"))
        assertTrue(MiuiIconsZip.belongsTo("fancy_icons/com.whatsapp/manifest.xml", "com.whatsapp"))
        assertTrue(MiuiIconsZip.belongsTo("res/drawable-xxhdpi/com.whatsapp/0.png", "com.whatsapp"))
    }

    @Test
    fun `belongsTo does not match other packages sharing a prefix`() {
        assertFalse(MiuiIconsZip.belongsTo("res/drawable-xxhdpi/com.whatsapp.w4b.png", "com.whatsapp"))
        assertFalse(MiuiIconsZip.belongsTo("res/drawable-xxhdpi/com.whatsapp2.png", "com.whatsapp"))
        assertFalse(MiuiIconsZip.belongsTo("transform_config.xml", "com.whatsapp"))
        // No extension: the whole file name is the stem, not "com".
        assertFalse(MiuiIconsZip.belongsTo("res/com.whatsapp", "com"))
    }

    @Test
    fun `rewrite swaps only the target package and keeps everything else`() {
        val base = MiuiIconsZip.write(
            linkedMapOf(
                "transform_config.xml" to "<cfg/>".encodeToByteArray(),
                "res/drawable-xxhdpi/com.whatsapp.png" to fakePng("wa-theme"),
                "fancy_icons/com.whatsapp/0.png" to fakePng("wa-fancy"),
                "res/drawable-xxhdpi/com.android.chrome.png" to fakePng("chrome"),
            ),
        )
        val custom = fakePng("custom")
        val out = MiuiIconsZip.read(
            MiuiIconsZip.rewrite(base, setOf("com.whatsapp"), mapOf("res/drawable-xxhdpi/com.whatsapp.png" to custom)),
        )
        assertEquals(
            setOf("transform_config.xml", "res/drawable-xxhdpi/com.android.chrome.png", "res/drawable-xxhdpi/com.whatsapp.png"),
            out.keys,
        )
        assertArrayEquals(custom, out["res/drawable-xxhdpi/com.whatsapp.png"])
        assertArrayEquals(fakePng("chrome"), out["res/drawable-xxhdpi/com.android.chrome.png"])
    }

    @Test
    fun `rewrite with no base creates a fresh icons zip`() {
        val out = MiuiIconsZip.read(MiuiIconsZip.rewrite(null, setOf("a.b"), mapOf("res/drawable-xxhdpi/a.b.png" to fakePng("x"))))
        assertEquals(setOf("res/drawable-xxhdpi/a.b.png"), out.keys)
    }

    @Test
    fun `detectIconDir picks the folder holding most flat icons`() {
        val entries = linkedMapOf(
            "res/drawable-xxxhdpi/a.b.png" to fakePng("1"),
            "res/drawable-xxxhdpi/c.d.png" to fakePng("2"),
            "res/drawable-xxhdpi/e.f.png" to fakePng("3"),
            "res/drawable-xxxhdpi/g.h/0.png" to fakePng("4"),
        )
        assertEquals("res/drawable-xxxhdpi", MiuiIconsZip.detectIconDir(entries))
        assertEquals(MiuiIconsZip.DEFAULT_ICON_DIR, MiuiIconsZip.detectIconDir(emptyMap()))
    }

    @Test
    fun `mtz contains description and icons component`() {
        val icons = MiuiIconsZip.write(mapOf("res/drawable-xxhdpi/a.b.png" to fakePng("x")))
        val mtz = MiuiIconsZip.read(MtzPackager.build(MtzPackager.Description(uiVersion = "816", title = "A & <B>"), icons))
        assertEquals(setOf("description.xml", "icons"), mtz.keys)
        assertArrayEquals(icons, mtz["icons"])
        val xml = mtz.getValue("description.xml").decodeToString()
        assertTrue(xml.contains("<uiVersion>816</uiVersion>"))
        assertTrue(xml.contains("<title>A &amp; &lt;B&gt;</title>"))
    }
}

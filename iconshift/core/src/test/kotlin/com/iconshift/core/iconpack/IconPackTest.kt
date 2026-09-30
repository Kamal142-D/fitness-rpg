package com.iconshift.core.iconpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPackTest {

    private fun sampleIndex(): IconPackIndex = IconPackIndexBuilder("com.example.pack").apply {
        onAppFilterItem("ComponentInfo{com.whatsapp/com.whatsapp.Main}", "whatsapp")
        onAppFilterItem("ComponentInfo{com.whatsapp/.HomeActivity}", "whatsapp_2")
        onAppFilterItem("ComponentInfo{com.instagram.android/com.instagram.mainactivity.MainActivity}", "instagram")
        onAppFilterItem("ComponentInfo{com.broken}", "orphan_drawable")
        onAppFilterItem("ComponentInfo{com.x/com.x.Y}", "")
        onDrawableItem("whatsapp_alt_03")
        onDrawableItem("whatsapp")
        onDrawableItem("camera")
        onDrawableItem("missing_resource")
    }.build { it != "missing_resource" }

    @Test
    fun `component keys are normalised`() {
        assertEquals("com.a/com.a.Main", ComponentKey.parse("ComponentInfo{com.a/com.a.Main}"))
        assertEquals("com.a/com.a.Main", ComponentKey.parse("ComponentInfo{com.a/.Main}"))
        assertNull(ComponentKey.parse("ComponentInfo{com.a}"))
        assertEquals("com.a/com.a.Main", ComponentKey.of("com.a", ".Main"))
    }

    @Test
    fun `builder dedupes, drops unresolvable names and keeps mappings`() {
        val index = sampleIndex()
        val names = index.entries.map { it.drawableName }
        assertEquals(listOf("whatsapp", "whatsapp_2", "instagram", "orphan_drawable", "whatsapp_alt_03", "camera"), names)
        assertEquals(listOf("whatsapp"), index.componentToDrawables["com.whatsapp/com.whatsapp.Main"])
        assertEquals(listOf("whatsapp_2"), index.componentToDrawables["com.whatsapp/com.whatsapp.HomeActivity"])
        assertEquals(listOf("com.whatsapp/com.whatsapp.Main"), index.entry("whatsapp")!!.mappedComponents)
        assertEquals("Whatsapp alt 03", index.entry("whatsapp_alt_03")!!.label)
        assertTrue("com.whatsapp" in index.mappedPackages)
    }

    @Test
    fun `recommend puts exact component first, then package mappings, then name matches`() {
        val rec = IconMatcher.recommend(sampleIndex(), "com.whatsapp", "com.whatsapp.Main", "WhatsApp").map { it.drawableName }
        assertEquals(listOf("whatsapp", "whatsapp_2", "whatsapp_alt_03"), rec)
    }

    @Test
    fun `recommend falls back to name matching for unmapped apps`() {
        val rec = IconMatcher.recommend(sampleIndex(), "com.oem.camera", null, "Camera").map { it.drawableName }
        assertEquals(listOf("camera"), rec)
    }

    @Test
    fun `search matches drawable names and mapped packages, ignoring case`() {
        val index = sampleIndex()
        assertEquals(listOf("whatsapp", "whatsapp_2", "whatsapp_alt_03"), IconMatcher.search(index, "WhatsApp").map { it.drawableName })
        assertEquals(listOf("instagram"), IconMatcher.search(index, "instagram.android").map { it.drawableName })
        assertEquals(index.entries.size, IconMatcher.search(index, "  ").size)
    }

    @Test
    fun `codec round-trips entries, order and mappings`() {
        val index = sampleIndex()
        val decoded = IconPackIndexCodec.decode(index.packPackage, IconPackIndexCodec.encode(index))!!
        assertEquals(index.entries, decoded.entries)
        assertEquals(index.componentToDrawables, decoded.componentToDrawables)
        assertNull(IconPackIndexCodec.decode("x", "not a cache file"))
    }

    @Test
    fun `recommend and search stay fast on a 30k-icon pack`() {
        val builder = IconPackIndexBuilder("com.big.pack")
        for (i in 0 until 30_000) {
            builder.onAppFilterItem("ComponentInfo{com.app$i/com.app$i.Main}", "icon_app_$i")
            builder.onDrawableItem("alt_style_${i}_variant")
        }
        builder.onAppFilterItem("ComponentInfo{com.whatsapp/com.whatsapp.Main}", "whatsapp")
        val index = builder.build()
        val start = System.nanoTime()
        val rec = IconMatcher.recommend(index, "com.whatsapp", "com.whatsapp.Main", "WhatsApp")
        repeat(10) { IconMatcher.search(index, "app1$it") }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertEquals("whatsapp", rec.first().drawableName)
        assertTrue("took ${ms}ms", ms < 3_000)
    }
}

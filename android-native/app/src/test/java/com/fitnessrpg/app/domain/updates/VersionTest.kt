package com.fitnessrpg.app.domain.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test
    fun `strips a leading v and prerelease or build metadata`() {
        assertEquals(listOf(1, 2, 3), parseVersion("v1.2.3"))
        assertEquals(listOf(1, 2, 3), parseVersion("1.2.3-beta.1"))
        assertEquals(listOf(1, 2), parseVersion("1.2"))
    }

    @Test
    fun `orders by numeric parts`() {
        assertEquals(0, compareVersions("1.2.0", "1.2.0"))
        assertEquals(1, compareVersions("1.3.0", "1.2.9"))
        assertEquals(-1, compareVersions("1.2.0", "1.10.0"))
    }

    @Test
    fun `treats missing parts as zero`() {
        assertEquals(0, compareVersions("1.2", "1.2.0"))
        assertEquals(1, compareVersions("1.2.1", "1.2"))
    }

    @Test
    fun `isNewerVersion is true only when latest strictly beats current`() {
        assertTrue(isNewerVersion("0.2.0", "0.1.0"))
        assertTrue(isNewerVersion("v0.1.1", "0.1.0"))
        assertFalse(isNewerVersion("0.1.0", "0.1.0"))
        assertFalse(isNewerVersion("0.1.0", "0.2.0"))
    }
}

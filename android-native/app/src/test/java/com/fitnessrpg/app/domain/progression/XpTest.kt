package com.fitnessrpg.app.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XpTest {
    @Test
    fun `matches the documented curve 100 times level to the 1_5`() {
        assertEquals(100, getXpRequiredForLevel(1))
        assertEquals(800, getXpRequiredForLevel(4))
        assertEquals(2700, getXpRequiredForLevel(9))
        assertEquals(6400, getXpRequiredForLevel(16))
    }

    @Test
    fun `is strictly increasing across many levels`() {
        for (l in 1 until 60) {
            assertTrue(getXpRequiredForLevel(l + 1) > getXpRequiredForLevel(l))
        }
    }

    @Test
    fun `treats sub-1 levels as level 1`() {
        assertEquals(100, getXpRequiredForLevel(0))
        assertEquals(100, getXpRequiredForLevel(-5))
    }

    @Test
    fun `reports current, required, and fraction within a level`() {
        val p = xpProgress(50, 1)
        assertEquals(100, p.required)
        assertEquals(50, p.current)
        assertEquals(0.5, p.fraction, 1e-9)
    }

    @Test
    fun `clamps current XP and fraction`() {
        val over = xpProgress(9999, 1)
        assertEquals(100, over.current)
        assertEquals(1.0, over.fraction, 1e-9)
        val under = xpProgress(-20, 1)
        assertEquals(0, under.current)
        assertEquals(0.0, under.fraction, 1e-9)
    }
}

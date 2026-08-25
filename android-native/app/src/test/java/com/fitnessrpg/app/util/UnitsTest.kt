package com.fitnessrpg.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test
    fun `converts kg to lb and back`() {
        assertEquals(220.462, kgToLb(100.0), 1e-3)
        assertEquals(100.0, lbToKg(220.462), 1e-3)
    }

    @Test
    fun `round-trips without drift`() {
        assertEquals(60.0, lbToKg(kgToLb(60.0)), 1e-6)
    }

    @Test
    fun `rounds to a given precision`() {
        assertEquals(220.5, roundTo(220.4622, 1), 1e-9)
        assertEquals(1.23, roundTo(1.2345, 2), 1e-9)
    }
}

package com.fitnessrpg.app.domain.ranking

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceGradeTest {

    @Test
    fun `is unscored with no baseline or no valid effort`() {
        assertEquals(0.0, performanceScore(120.0, null), 1e-9)
        assertEquals(0.0, performanceScore(null, 100.0), 1e-9)
    }

    @Test
    fun `scores at, above, and below the baseline`() {
        assertEquals(65.0, performanceScore(100.0, 100.0), 1e-4) // ratio 1.0
        assertEquals(88.0, performanceScore(110.0, 100.0), 1e-4) // ratio 1.1
        assertEquals(40.0, performanceScore(90.0, 100.0), 1e-4) // ratio 0.9
    }

    @Test
    fun `uses words and never a temporary rank letter`() {
        assertEquals("Baseline", todayPerformanceLabel(100.0, null))
        assertEquals("Normal", todayPerformanceLabel(100.0, 100.0))
        assertEquals("PR", todayPerformanceLabel(110.0, 100.0, isPr = true))
    }
}

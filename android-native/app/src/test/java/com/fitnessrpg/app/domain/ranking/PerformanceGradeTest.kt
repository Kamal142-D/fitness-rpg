package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceGradeTest {

    @Test
    fun `is neutral with no baseline or no valid effort`() {
        assertEquals(60.0, performanceScore(120.0, null), 1e-9)
        assertEquals(60.0, performanceScore(null, 100.0), 1e-9)
    }

    @Test
    fun `scores at, above, and below the baseline`() {
        assertEquals(65.0, performanceScore(100.0, 100.0), 1e-4) // ratio 1.0
        assertEquals(88.0, performanceScore(110.0, 100.0), 1e-4) // ratio 1.1
        assertEquals(40.0, performanceScore(90.0, 100.0), 1e-4) // ratio 0.9
    }

    @Test
    fun `maps to a grade letter`() {
        assertEquals(Rank.A, performanceGrade(performanceScore(100.0, 100.0))) // 65 -> A
        assertEquals(Rank.C, performanceGrade(performanceScore(90.0, 100.0))) // 40 -> C
    }
}

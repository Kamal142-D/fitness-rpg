package com.fitnessrpg.app.domain.rankings

import org.junit.Assert.assertEquals
import org.junit.Test

class Estimated1RMTest {
    @Test
    fun `epley formula`() {
        assertEquals(100.0, calculateEstimated1RM(100.0, 1), 1e-9)
        assertEquals(116.6667, calculateEstimated1RM(100.0, 5), 1e-3)
        assertEquals(133.3333, calculateEstimated1RM(100.0, 10), 1e-3)
    }

    @Test
    fun `guards invalid input`() {
        assertEquals(0.0, calculateEstimated1RM(0.0, 5), 1e-9)
        assertEquals(0.0, calculateEstimated1RM(-50.0, 5), 1e-9)
        assertEquals(0.0, calculateEstimated1RM(100.0, 0), 1e-9)
    }
}

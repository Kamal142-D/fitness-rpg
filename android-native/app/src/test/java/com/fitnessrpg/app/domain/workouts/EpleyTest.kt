package com.fitnessrpg.app.domain.workouts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpleyTest {
    @Test
    fun `returns the weight itself at 1 rep`() {
        assertEquals(100.0, estimatedOneRepMax(100.0, 1)!!, 1e-9)
    }

    @Test
    fun `applies weight times one plus reps over thirty`() {
        assertEquals(116.67, estimatedOneRepMax(100.0, 5)!!, 1e-9)
        assertEquals(133.33, estimatedOneRepMax(100.0, 10)!!, 1e-9)
    }

    @Test
    fun `rejects reps outside 1 to 12 and non-positive weight`() {
        assertNull(estimatedOneRepMax(100.0, 0))
        assertNull(estimatedOneRepMax(100.0, 13))
        assertNull(estimatedOneRepMax(0.0, 5))
        assertNull(estimatedOneRepMax(-50.0, 5))
    }

    @Test
    fun `returns null when weight or reps is null`() {
        assertNull(estimatedOneRepMax(null, 5))
        assertNull(estimatedOneRepMax(100.0, null))
    }
}

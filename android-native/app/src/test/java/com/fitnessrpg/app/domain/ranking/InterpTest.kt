package com.fitnessrpg.app.domain.ranking

import org.junit.Assert.assertEquals
import org.junit.Test

class InterpTest {
    private val anchors = listOf(Anchor(0.0, 0.0), Anchor(10.0, 100.0))

    @Test
    fun `interpolates linearly between anchors`() {
        assertEquals(50.0, interpolate(anchors, 5.0), 1e-9)
        assertEquals(25.0, interpolate(anchors, 2.5), 1e-9)
    }

    @Test
    fun `clamps below the first and above the last anchor`() {
        assertEquals(0.0, interpolate(anchors, -5.0), 1e-9)
        assertEquals(100.0, interpolate(anchors, 999.0), 1e-9)
    }

    @Test
    fun `handles a single anchor and empty input`() {
        assertEquals(42.0, interpolate(listOf(Anchor(1.0, 42.0)), 5.0), 1e-9)
        assertEquals(0.0, interpolate(emptyList(), 5.0), 1e-9)
    }
}

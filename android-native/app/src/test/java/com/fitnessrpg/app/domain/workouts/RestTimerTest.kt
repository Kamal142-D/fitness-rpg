package com.fitnessrpg.app.domain.workouts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {
    @Test
    fun `is 0 when there is no rest end`() {
        assertEquals(0, restRemainingSeconds(null, 1000L))
    }

    @Test
    fun `rounds up remaining time and floors at 0`() {
        assertEquals(11, restRemainingSeconds(10_500L, 0L))
        assertEquals(0, restRemainingSeconds(500L, 1000L))
    }

    @Test
    fun `isResting reflects remaining greater than zero`() {
        assertTrue(isResting(5000L, 0L))
        assertFalse(isResting(0L, 1000L))
    }

    @Test
    fun `formats seconds as minutes and seconds`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:09", formatClock(9))
        assertEquals("1:15", formatClock(75))
        assertEquals("10:00", formatClock(600))
    }
}

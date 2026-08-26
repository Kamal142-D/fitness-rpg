package com.fitnessrpg.app.domain.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyMarchTest {
    private val today = LocalDate.of(2026, 8, 26)

    @Test
    fun `first sensor sample establishes baseline without inventing steps`() {
        val result = accumulateSensorSample(SensorAccumulator(today), 12_000, today)
        assertEquals(12_000L, result.lastBootStepCount)
        assertEquals(0, result.accumulatedSteps)
    }

    @Test
    fun `same-day sensor delta accumulates`() {
        val current = SensorAccumulator(today, lastBootStepCount = 12_000, accumulatedSteps = 1_500)
        val result = accumulateSensorSample(current, 12_240, today)
        assertEquals(1_740, result.accumulatedSteps)
    }

    @Test
    fun `new day resets steps and establishes a fresh baseline`() {
        val yesterday = today.minusDays(1)
        val result = accumulateSensorSample(SensorAccumulator(yesterday, 8_000, 7_000), 8_500, today)
        assertEquals(0, result.accumulatedSteps)
        assertEquals(8_500L, result.lastBootStepCount)
    }

    @Test
    fun `device reboot does not create a negative step delta`() {
        val result = accumulateSensorSample(SensorAccumulator(today, 20_000, 2_000), 30, today)
        assertEquals(2_000, result.accumulatedSteps)
        assertEquals(30L, result.lastBootStepCount)
    }

    @Test
    fun `goal progress is clamped and estimates remain non-negative`() {
        assertEquals(1f, stepGoalFraction(12_000, 8_000))
        assertEquals(0f, stepGoalFraction(-100, 8_000))
        assertTrue(estimatedDistanceKm(8_000) > 0.0)
        assertEquals(0, estimatedActiveMinutes(-10))
    }

    @Test
    fun `streak may end today or yesterday while today is still open`() {
        val history = listOf(
            DailyStepProgress(today.minusDays(1), 8_000, 8_000),
            DailyStepProgress(today.minusDays(2), 9_000, 8_000),
            DailyStepProgress(today.minusDays(3), 4_000, 8_000),
        )
        assertEquals(2, completedStepStreak(history, today))
        assertEquals(3, completedStepStreak(history + DailyStepProgress(today, 8_000, 8_000), today))
    }
}

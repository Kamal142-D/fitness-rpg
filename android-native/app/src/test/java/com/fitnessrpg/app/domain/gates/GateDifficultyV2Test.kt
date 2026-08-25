package com.fitnessrpg.app.domain.gates

import org.junit.Assert.*
import org.junit.Test

class GateDifficultyV2Test {
    @Test fun `gate score uses exact component weights`() {
        val result = calculateGateDifficulty(
            listOf(
                ExerciseDifficultyInput(
                    "bench",
                    listOf(DifficultySet(70.0, 8, 8.0, false), DifficultySet(70.0, 8, 8.0, false)),
                    currentEstimated1rmKg = 100.0,
                    recentAverageVolumeKg = 1000.0,
                    priorSessionCount = 3,
                ),
            ),
            workoutDurationMinutes = 10.0,
        )
        val expected = result.intensityScore * .45 + result.hardSetsScore * .25 + result.volumeScore * .20 + result.densityScore * .10
        assertEquals(expected, result.score, 1e-9)
        assertFalse(result.provisional)
    }

    @Test fun `missing personal history produces provisional difficulty`() {
        val result = calculateGateDifficulty(
            listOf(ExerciseDifficultyInput("bench", listOf(DifficultySet(40.0, 8, 8.0, false)), null)),
            workoutDurationMinutes = 20.0,
        )
        assertTrue(result.provisional)
        assertEquals("low", result.confidence)
    }
}

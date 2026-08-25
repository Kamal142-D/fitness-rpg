package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GateDifficultyTest {
    private val working = listOf(
        DifficultySet(45.0, 8, 8.0, false),
        DifficultySet(45.0, 8, 8.0, false),
        DifficultySet(45.0, 8, 9.0, false),
    )

    @Test fun `same work is harder for weaker user`() {
        val weaker = calculateExerciseDifficulty(ExerciseDifficultyInput("bench", working, 60.0))
        val stronger = calculateExerciseDifficulty(ExerciseDifficultyInput("bench", working, 110.0))
        assertTrue(weaker.score > stronger.score)
    }

    @Test fun `warmups do not change difficulty`() {
        val base = calculateExerciseDifficulty(ExerciseDifficultyInput("bench", working, 80.0))
        val withWarmups = calculateExerciseDifficulty(
            ExerciseDifficultyInput("bench", listOf(DifficultySet(20.0, 12, 3.0, true)) + working, 80.0),
        )
        assertEquals(base.score, withWarmups.score, 0.001)
    }

    @Test fun `more weight and reps generally increase difficulty`() {
        val easy = calculateExerciseDifficulty(ExerciseDifficultyInput("bench", listOf(DifficultySet(30.0, 5, 6.0, false)), 80.0))
        val hard = calculateExerciseDifficulty(ExerciseDifficultyInput("bench", listOf(DifficultySet(50.0, 10, 9.0, false)), 80.0))
        assertTrue(hard.score > easy.score)
    }

    @Test fun `bodyweight load is not zero`() {
        val result = calculateExerciseDifficulty(
            ExerciseDifficultyInput("pullup", listOf(DifficultySet(0.0, 8, 8.0, false)), 90.0, bodyWeightKg = 75.0, equipment = "body weight"),
        )
        assertTrue(result.workingVolumeKg > 0.0)
    }

    @Test fun `difficulty and clear rank can differ`() {
        val difficulty = calculateGateDifficulty(listOf(ExerciseDifficultyInput("bench", working, 75.0)))
        val clearRank = Rank.A
        assertTrue(difficulty.rank != clearRank || difficulty.score in 65.0..79.999)
    }
}

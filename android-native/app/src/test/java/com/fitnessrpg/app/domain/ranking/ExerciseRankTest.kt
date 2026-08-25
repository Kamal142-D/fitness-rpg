package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRankTest {

    @Test
    fun `maps a 1x-bodyweight bench (male) to the middle of the scale`() {
        val score = exerciseScore(
            ExerciseScoreInput("Barbell Bench Press", 80.0, 80.0, "male"),
        )
        assertEquals(50.0, score!!, 1e-9)
        assertEquals(Rank.B, permanentExerciseRank(score))
    }

    @Test
    fun `returns null for exercises with no strength standard`() {
        assertNull(exerciseScore(ExerciseScoreInput("Lateral Raise", 30.0, 80.0, "male")))
    }

    @Test
    fun `returns null when bodyweight or 1RM is missing`() {
        assertNull(exerciseScore(ExerciseScoreInput("Barbell Bench Press", null, 80.0, "male")))
        assertNull(exerciseScore(ExerciseScoreInput("Barbell Bench Press", 80.0, 0.0, "male")))
    }

    @Test
    fun `scales lighter for female standards`() {
        val male = exerciseScore(ExerciseScoreInput("Barbell Bench Press", 80.0, 80.0, "male"))!!
        val female = exerciseScore(ExerciseScoreInput("Barbell Bench Press", 80.0, 80.0, "female"))!!
        assertTrue(female > male)
    }

    @Test
    fun `requires two qualifying sessions to reach A or S`() {
        assertEquals(Rank.B, nextExerciseRank(null, 90.0, 1))
        assertEquals(Rank.S, nextExerciseRank(null, 90.0, 2))
    }

    @Test
    fun `caps a single update to plus two rank bands`() {
        assertEquals(Rank.C, nextExerciseRank(Rank.E, 90.0, 5))
    }

    @Test
    fun `never decreases (permanent rank is a high-water mark)`() {
        assertEquals(Rank.B, nextExerciseRank(Rank.B, 5.0, 5))
    }

    @Test
    fun `allows A to S only with two sessions`() {
        assertEquals(Rank.A, nextExerciseRank(Rank.A, 85.0, 1))
        assertEquals(Rank.S, nextExerciseRank(Rank.A, 85.0, 2))
    }
}

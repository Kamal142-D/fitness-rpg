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
    fun `personal exercise is unranked until three valid sessions`() {
        val first = calculatePersonalExerciseTier(50.0, emptyList())
        val second = calculatePersonalExerciseTier(52.0, listOf(50.0))
        assertNull(first.rank)
        assertNull(second.rank)
        assertEquals(1, first.baselineSessions)
        assertEquals(2, second.baselineSessions)
    }

    @Test
    fun `third personal session establishes robust baseline and low tier cap`() {
        val ranked = calculatePersonalExerciseTier(200.0, listOf(50.0, 52.0))
        assertEquals(Rank.C, ranked.rank)
        assertEquals(3, ranked.baselineSessions)
        assertTrue(!ranked.globallyComparable)
    }

    @Test
    fun `machine movement uses personal mode rather than a fake global rank`() {
        val result = calculateExerciseRanking(
            ExerciseScoreInput("Cable Row", 70.0, 80.0, "male", com.fitnessrpg.app.domain.rankings.Equipment.CABLE),
            listOf(60.0, 62.0),
        )
        assertEquals(com.fitnessrpg.app.domain.rankings.ExerciseRankingMode.PERSONAL, result.mode)
        assertTrue(!result.globallyComparable)
    }

    @Test
    fun `standardized barbell bench uses global mode`() {
        val result = calculateExerciseRanking(
            ExerciseScoreInput("Barbell Bench Press", 80.0, 80.0, "male", com.fitnessrpg.app.domain.rankings.Equipment.BARBELL),
        )
        assertEquals(com.fitnessrpg.app.domain.rankings.ExerciseRankingMode.GLOBAL, result.mode)
        assertTrue(result.globallyComparable)
    }
}

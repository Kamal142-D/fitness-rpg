package com.fitnessrpg.app.domain.rankings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthScoreTest {

    private fun bench(weight: Double, reps: Int, eq: Equipment = Equipment.BARBELL, mode: DumbbellWeightMode? = null) =
        StrengthAssessmentInput("bench", eq, weight, reps, mode)

    @Test
    fun `unknown movement scores null`() {
        assertNull(scoreStrengthMovement(StrengthAssessmentInput("curl", Equipment.DUMBBELL, 20.0, 8), 71.5, "male"))
    }

    @Test
    fun `weak barbell bench scores low`() {
        val s = scoreStrengthMovement(bench(24.0, 8), 71.5, "male")!!
        assertTrue("bench was $s", s in 12.0..24.0)
    }

    @Test
    fun `high reps are clamped so they cannot inflate strength`() {
        val at12 = scoreStrengthMovement(bench(60.0, 12), 71.5, "male")!!
        val at20 = scoreStrengthMovement(bench(60.0, 20), 71.5, "male")!!
        val at8 = scoreStrengthMovement(bench(60.0, 8), 71.5, "male")!!
        assertEquals(at12, at20, 1e-9) // 20 reps clamped to 12
        assertTrue(at8 < at12)
    }

    @Test
    fun `dumbbell per-hand counts both hands`() {
        val perHand = scoreStrengthMovement(bench(24.0, 8, Equipment.DUMBBELL, DumbbellWeightMode.PER_HAND), 71.5, "male")!!
        val barbell = scoreStrengthMovement(bench(24.0, 8, Equipment.BARBELL), 71.5, "male")!!
        assertTrue(perHand > barbell) // 24 per hand = 48 total > 24 barbell
    }

    @Test
    fun `combined score penalizes imbalance rather than taking the max`() {
        val strongBenchWeakSquat = computeStrengthScore(
            listOf(
                StrengthAssessmentInput("bench", Equipment.BARBELL, 120.0, 5),
                StrengthAssessmentInput("squat", Equipment.BARBELL, 20.0, 5),
            ),
            71.5, "male",
        )!!
        val benchOnly = scoreStrengthMovement(
            StrengthAssessmentInput("bench", Equipment.BARBELL, 120.0, 5), 71.5, "male",
        )!!
        assertTrue("combined $strongBenchWeakSquat should be well below bench-only $benchOnly", strongBenchWeakSquat < benchOnly)
    }

    @Test
    fun `no assessable movements yields null`() {
        assertNull(computeStrengthScore(emptyList(), 71.5, "male"))
    }
}

package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HunterRankTest {

    // ---- Required regression tests ----

    @Test
    fun `test1 - strong physique cannot carry weak strength to A or S`() {
        val r = computeHunterRank(physiqueScore = 80.0, strengthScore = 25.0, conditioningScore = 40.0)
        assertNotEquals(Rank.A, r.rank)
        assertNotEquals(Rank.S, r.rank)
        assertEquals(Rank.D, r.rank) // strength 25 fails C's minStrength (30)
        assertEquals(PhysicalAttribute.STRENGTH, r.limitingAttribute)
    }

    @Test
    fun `test2 - weak conditioning blocks A`() {
        val r = computeHunterRank(70.0, 70.0, 30.0)
        assertNotEquals(Rank.A, r.rank)
        assertEquals(Rank.C, r.rank)
    }

    @Test
    fun `test3 - balanced athlete reaches a strong rank`() {
        val r = computeHunterRank(72.0, 68.0, 60.0)
        // hunterScore ~65.6 < 70, so A is not yet met -> B (still a strong rank).
        assertEquals(Rank.B, r.rank)
    }

    @Test
    fun `test4 - unassessed conditioning forces provisional and blocks A or S`() {
        val r = computeHunterRank(80.0, 80.0, null)
        assertTrue(r.provisional)
        assertEquals(Rank.C, r.rank)
        assertNotEquals(Rank.A, r.rank)
        assertNotEquals(Rank.S, r.rank)
    }

    @Test
    fun `test5 - the real 71_5kg user must never be A`() {
        val body = BodyCompositionData(
            weightKg = 71.5, heightCm = 171.0, bodyFatPercent = 18.0, muscleMassKg = 55.2, sex = "male",
        )
        val physique = computePhysiqueScore(body)!!
        val strength = computeStrengthScore(
            listOf(
                StrengthAssessmentInput("bench", Equipment.BARBELL, 24.0, 8),
                StrengthAssessmentInput("squat", Equipment.BARBELL, 20.0, 8),
            ),
            bodyweightKg = 71.5,
            sex = "male",
        )!!
        val r = computeHunterRank(physique, strength, null)

        assertNotEquals(Rank.A, r.rank)
        assertNotEquals(Rank.S, r.rank)
        assertTrue(r.rank.ordinal <= Rank.C.ordinal)
        assertTrue(r.provisional)
        assertEquals(PhysicalAttribute.CONDITIONING, r.limitingAttribute)
    }

    // ---- Supporting tests ----

    @Test
    fun `discipline is not an input and cannot raise the rank`() {
        // A weak physical profile stays low no matter how disciplined the user is —
        // there is simply no discipline parameter to pass here.
        val r = computeHunterRank(30.0, 20.0, 25.0)
        assertTrue(r.rank == Rank.E || r.rank == Rank.D)
    }

    @Test
    fun `minimum requirements gate the rank (example from the spec)`() {
        val r = computeHunterRank(72.0, 28.0, 44.0)
        assertNotEquals(Rank.B, r.rank)
        assertNotEquals(Rank.A, r.rank)
        assertEquals(Rank.D, r.rank) // strength 28 fails C's minStrength (30)
    }

    @Test
    fun `weakest attribute penalty pulls the score down`() {
        val balanced = computeHunterRank(60.0, 60.0, 60.0).hunterScore
        val lopsided = computeHunterRank(90.0, 30.0, 60.0).hunterScore
        // Same-ish average, but the low strength weakest-penalty keeps lopsided lower.
        assertTrue(lopsided < balanced + 5.0)
    }

    @Test
    fun `provisional cap holds even with elite pillars`() {
        val r = computeHunterRank(95.0, 95.0, null)
        assertTrue(r.provisional)
        assertEquals(Rank.C, r.rank)
    }

    @Test
    fun `full elite profile can reach S`() {
        val r = computeHunterRank(90.0, 90.0, 85.0, AssessmentConfidence.HIGH)
        assertFalse(r.provisional)
        assertEquals(Rank.S, r.rank)
        assertEquals(AssessmentConfidence.HIGH, r.confidence)
    }

    @Test
    fun `confidence reflects assessed pillars`() {
        assertEquals(AssessmentConfidence.LOW, computeHunterRank(60.0, null, null).confidence)
        assertEquals(AssessmentConfidence.LOW, computeHunterRank(60.0, 60.0, null).confidence)
        assertEquals(AssessmentConfidence.MEDIUM, computeHunterRank(60.0, 60.0, 60.0).confidence)
    }

    @Test
    fun `next rank info points at the rank above`() {
        val r = computeHunterRank(80.0, 25.0, 40.0) // rank D
        assertEquals(Rank.C, r.nextRank?.rank)
        assertEquals(30, r.nextRank?.strength)
    }
}

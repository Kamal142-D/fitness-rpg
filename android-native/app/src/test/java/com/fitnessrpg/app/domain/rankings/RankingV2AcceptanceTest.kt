package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.*
import org.junit.Test

class RankingV2AcceptanceTest {
    @Test fun `unknown pillars remain null and provisional`() {
        val result = computeHunterRank(physiqueScore = null, strengthScore = null, conditioningScore = null)
        assertNull(result.hunterScore)
        assertEquals(Rank.E, result.rank)
        assertTrue(result.provisional)
        assertEquals(Rank.C, result.rankCap)
    }

    @Test fun `balanced complete medium-confidence athlete may reach A but never S`() {
        val result = computeHunterRank(78.0, 78.0, 72.0, AssessmentConfidence.MEDIUM)
        assertEquals(Rank.A, result.rank)
        assertFalse(result.provisional)
        assertEquals(Rank.A, result.rankCap)
    }

    @Test fun `segmental balance is required only for S physique`() {
        val base = BodyCompositionData(
            weightKg = 82.0, heightCm = 180.0, bodyFatPercent = 12.0,
            leanBodyMassKg = 72.0, waistCm = 72.0, sex = "male", ageYears = 30,
            assessedAtEpochDay = 100,
        )
        val missing = computePhysiqueRank(base, 100)
        assertEquals(Rank.A, missing.rankCap)
        val measured = computePhysiqueRank(
            base.copy(segmentalLeanMass = SegmentalLeanMassData(4.2, 4.2, 10.5, 10.5)), 100,
        )
        assertEquals(Rank.S, measured.rankCap)
        assertEquals(AssessmentConfidence.HIGH, measured.confidence)
    }

    @Test fun `dumbbell strength without weight mode is not scored`() {
        assertNull(scoreStrengthMovement(StrengthAssessmentInput("bench", Equipment.DUMBBELL, 30.0, 8), 80.0, "male"))
    }

    @Test fun `A and S strength require repeated sessions`() {
        fun evidence(session: String) = listOf(
            StrengthAssessmentInput("bench", Equipment.BARBELL, 115.0, 5, variation = "flat", performedAtEpochDay = 100, sessionId = session),
            StrengthAssessmentInput("squat", Equipment.BARBELL, 155.0, 5, variation = "back", performedAtEpochDay = 100, sessionId = session),
            StrengthAssessmentInput("deadlift", Equipment.BARBELL, 185.0, 5, variation = "conventional", performedAtEpochDay = 100, sessionId = session),
        )
        val once = computeStrengthRank(evidence("one"), 80.0, "male", todayEpochDay = 100)
        val repeated = computeStrengthRank(evidence("one") + evidence("two"), 80.0, "male", todayEpochDay = 100)
        assertTrue(once.rank!!.ordinal <= Rank.B.ordinal)
        assertTrue(repeated.rank!!.ordinal >= Rank.A.ordinal)
        assertEquals(AssessmentConfidence.HIGH, repeated.confidence)
    }

    @Test fun `all standardized conditioning tests are supported and current evidence is high-confidence`() {
        val cases = listOf(
            ConditioningInput(ConditioningTestType.COOPER_12_MINUTE, 2500.0, 30, "male", 100),
            ConditioningInput(ConditioningTestType.RUN_1_5_MILE, 11.0, 30, "male", 100),
            ConditioningInput(ConditioningTestType.STEP_3_MINUTE, 90.0, 30, "male", 100),
        )
        cases.forEach {
            val result = computeConditioningRank(it, 100)
            assertNotNull(result.score)
            assertFalse(result.provisional)
            assertEquals(AssessmentConfidence.HIGH, result.confidence)
        }
    }
}

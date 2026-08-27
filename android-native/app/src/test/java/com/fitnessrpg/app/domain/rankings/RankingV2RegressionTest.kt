package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.*
import org.junit.Test

class RankingV2RegressionTest {
    @Test fun `one great attribute cannot create elite Hunter`() {
        assertTrue(computeHunterRank(90.0, 25.0, 30.0, AssessmentConfidence.HIGH).rank.ordinal < Rank.A.ordinal)
    }

    @Test fun `strong but unconditioned athlete is not A`() {
        assertTrue(computeHunterRank(75.0, 80.0, 35.0, AssessmentConfidence.HIGH).rank.ordinal < Rank.A.ordinal)
    }

    @Test fun `balanced A is allowed only with sufficient score and confidence`() {
        assertEquals(Rank.A, computeHunterRank(78.0, 78.0, 72.0, AssessmentConfidence.MEDIUM).rank)
    }

    @Test fun `S requires complete recent high confidence evidence`() {
        assertTrue(computeHunterRank(90.0, 90.0, 86.0, AssessmentConfidence.HIGH).rank.ordinal >= Rank.S.ordinal)
        assertTrue(computeHunterRank(90.0, 90.0, 86.0, AssessmentConfidence.MEDIUM).rank.ordinal < Rank.S.ordinal)
    }

    @Test fun `incomplete strength input has no reliable score`() {
        val result = computeStrengthRank(emptyList(), 71.5, "male")
        assertNull(result.score)
        assertTrue(result.provisional)
    }

    @Test fun `stale evidence lowers confidence`() {
        val conditioning = computeConditioningRank(ConditioningInput(ConditioningTestType.COOPER_12_MINUTE, 2500.0, 30, "male", 1), 100)
        assertEquals(AssessmentConfidence.LOW, conditioning.confidence)
        assertTrue(conditioning.reasons.any { it.contains("update", ignoreCase = true) })
    }
}

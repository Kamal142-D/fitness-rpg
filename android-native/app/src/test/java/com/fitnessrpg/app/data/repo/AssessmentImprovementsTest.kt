package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.RankConfidence
import com.fitnessrpg.app.domain.rankings.ConditioningRankResult
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.domain.rankings.PhysiqueRankResult
import com.fitnessrpg.app.domain.rankings.StrengthRankResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentImprovementsTest {

    @Test
    fun `reports exact pillar and movement improvements`() {
        val previous = snapshot(
            hunterScore = 41.0,
            hunterRp = 66,
            strengthScore = 28.0,
            conditioningScore = 36.0,
            conditioningRank = Rank.C,
            movementScores = mapOf("HORIZONTAL_PUSH" to 30.0),
        )
        val current = snapshot(
            hunterScore = 48.0,
            hunterRp = 92,
            strengthScore = 39.0,
            conditioningScore = 52.0,
            conditioningRank = Rank.B,
            movementScores = mapOf("HORIZONTAL_PUSH" to 44.0),
        )

        val report = compareAssessments(previous, current)

        assertTrue(report.improvements.any { it.metric == "Hunter score" && it.delta == "+7 pts" })
        assertTrue(report.improvements.any { it.metric == "Strength score" && it.delta == "+11 pts" })
        assertTrue(report.improvements.any { it.metric == "Horizontal push" && it.delta == "+14 pts" })
        assertTrue(report.improvements.any { it.metric == "Conditioning rank" && it.current == "B" })
    }

    @Test
    fun `newly scorable evidence is a baseline not a claimed improvement`() {
        val previous = snapshot(
            hunterScore = 35.0,
            conditioningScore = null,
            conditioningRank = null,
            movementScores = emptyMap(),
        )
        val current = snapshot(
            hunterScore = 42.0,
            conditioningScore = 45.0,
            conditioningRank = Rank.C,
            movementScores = mapOf("KNEE_DOMINANT" to 38.0),
        )

        val report = compareAssessments(previous, current)

        assertTrue(report.newBaselines.any { it.metric == "Conditioning score" })
        assertTrue(report.newBaselines.any { it.metric == "Conditioning rank" })
        assertTrue(report.newBaselines.any { it.metric == "Knee dominant" })
        assertFalse(report.improvements.any { it.metric == "Conditioning score" })
    }

    @Test
    fun `lower ranked values are not listed as improvements`() {
        val previous = snapshot(hunterScore = 50.0, strengthScore = 45.0, movementScores = mapOf("HIP_HINGE" to 55.0))
        val current = snapshot(hunterScore = 47.0, strengthScore = 40.0, movementScores = mapOf("HIP_HINGE" to 50.0))

        val report = compareAssessments(previous, current)

        assertFalse(report.improvements.any { it.metric in setOf("Hunter score", "Strength score", "Hip hinge") })
    }

    private fun snapshot(
        hunterScore: Double,
        hunterRp: Int = 50,
        strengthScore: Double = 30.0,
        conditioningScore: Double? = 35.0,
        conditioningRank: Rank? = Rank.C,
        movementScores: Map<String, Double> = mapOf("HORIZONTAL_PUSH" to 30.0),
    ): RankAssessmentSnapshot {
        val physique = PhysiqueRankResult(
            score = 55.0,
            rank = Rank.B,
            bodyCompositionScore = 55.0,
            muscularityScore = 55.0,
            waistScore = 55.0,
            balanceScore = null,
            rankCap = Rank.A,
            provisional = false,
            confidence = RankConfidence.MEDIUM,
            reasons = emptyList(),
            rp = 25,
        )
        val strength = StrengthRankResult(
            score = strengthScore,
            rank = Rank.C,
            movementScores = movementScores,
            rankCap = Rank.A,
            provisional = false,
            confidence = RankConfidence.MEDIUM,
            reasons = emptyList(),
            rp = 30,
        )
        val conditioning = ConditioningRankResult(
            score = conditioningScore,
            rank = conditioningRank,
            provisional = conditioningScore == null,
            confidence = if (conditioningScore == null) RankConfidence.LOW else RankConfidence.HIGH,
            reasons = emptyList(),
            rp = if (conditioningScore == null) 0 else 40,
        )
        val hunter = HunterRankResult(
            rank = Rank.C,
            hunterScore = hunterScore,
            physiqueScore = physique.score,
            strengthScore = strength.score,
            conditioningScore = conditioning.score,
            limitingAttribute = null,
            provisional = false,
            confidence = RankConfidence.MEDIUM,
            nextRank = null,
            physique = physique,
            strength = strength,
            conditioning = conditioning,
            rp = hunterRp,
        )
        return RankAssessmentSnapshot(
            hunter = hunter,
            physique = physique,
            strength = strength,
            conditioning = conditioning,
            profile = null,
            latestBody = null,
        )
    }
}

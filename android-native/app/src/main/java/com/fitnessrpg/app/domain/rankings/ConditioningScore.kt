package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.ranking.Anchor
import com.fitnessrpg.app.domain.ranking.interpolate

data class ConditioningInput(
    val testType: ConditioningTestType? = null,
    val result: Double? = null,
    val ageYears: Int? = null,
    val sex: String? = null,
    val assessedAtEpochDay: Long? = null,
    val workCapacityScore: Double? = null,
)

private val cooperMale = listOf(Anchor(1200.0, 5.0), Anchor(1800.0, 30.0), Anchor(2200.0, 50.0), Anchor(2600.0, 70.0), Anchor(3000.0, 88.0), Anchor(3400.0, 100.0))
private val cooperFemale = listOf(Anchor(1000.0, 5.0), Anchor(1500.0, 30.0), Anchor(1900.0, 50.0), Anchor(2300.0, 70.0), Anchor(2700.0, 88.0), Anchor(3100.0, 100.0))
private val runMinutes = listOf(Anchor(7.0, 100.0), Anchor(9.0, 85.0), Anchor(11.0, 68.0), Anchor(13.0, 50.0), Anchor(16.0, 28.0), Anchor(22.0, 5.0))
private val recoveryHeartRate = listOf(Anchor(55.0, 100.0), Anchor(75.0, 85.0), Anchor(95.0, 65.0), Anchor(115.0, 42.0), Anchor(140.0, 10.0))

fun computeConditioningRank(input: ConditioningInput?, todayEpochDay: Long? = null): ConditioningRankResult {
    if (input == null || input.testType == null || input.result == null || input.ageYears == null || input.sex == null) {
        return ConditioningRankResult(null, null, true, AssessmentConfidence.LOW, listOf("A standardized conditioning test has not been completed."))
    }
    val raw = when (input.testType) {
        ConditioningTestType.COOPER_12_MINUTE -> interpolate(if (input.sex == "female") cooperFemale else cooperMale, input.result)
        ConditioningTestType.RUN_1_5_MILE -> interpolate(runMinutes, input.result)
        ConditioningTestType.STEP_3_MINUTE -> interpolate(recoveryHeartRate, input.result)
    }
    val ageAdjustment = ((input.ageYears - 30).coerceAtLeast(0) / 10) * 2.0
    val score = clampScore(raw + ageAdjustment)
    val stale = todayEpochDay != null && input.assessedAtEpochDay?.let { todayEpochDay - it > 90 } != false
    return ConditioningRankResult(score, scoreToRank(score), stale, if (stale) AssessmentConfidence.LOW else AssessmentConfidence.MEDIUM, if (stale) listOf("Conditioning assessment update recommended.") else emptyList())
}

fun computeConditioningScore(input: ConditioningInput?): Double? = computeConditioningRank(input).score

package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank
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

private val cooperReference = listOf(Anchor(900.0, 0.0), Anchor(1400.0, 20.0), Anchor(1900.0, 40.0), Anchor(2300.0, 60.0), Anchor(2700.0, 80.0), Anchor(3200.0, 100.0))
private val runMinutesReference = listOf(Anchor(7.0, 100.0), Anchor(9.0, 85.0), Anchor(11.0, 68.0), Anchor(13.0, 50.0), Anchor(16.0, 28.0), Anchor(22.0, 5.0))
private val recoveryHeartRateReference = listOf(Anchor(55.0, 100.0), Anchor(75.0, 85.0), Anchor(95.0, 65.0), Anchor(115.0, 42.0), Anchor(140.0, 10.0), Anchor(180.0, 0.0))

private fun conditioningAgeScale(age: Int): Double = when (age) {
    in 13..39 -> 1.0
    in 40..59 -> .94
    in 60..120 -> .86
    else -> Double.NaN
}

/** Normalize one standardized test by age and sex. Attendance is never an input. */
fun computeConditioningRank(input: ConditioningInput?, todayEpochDay: Long? = null): ConditioningRankResult {
    if (input?.testType == null || input.result == null || input.ageYears == null || input.sex !in setOf("male", "female")) {
        return ConditioningRankResult(null, null, true, AssessmentConfidence.LOW, listOf("A standardized conditioning test has not been completed."))
    }
    val result = input.result
    val plausible = when (input.testType) {
        ConditioningTestType.COOPER_12_MINUTE -> result in 500.0..5000.0
        ConditioningTestType.RUN_1_5_MILE -> result in 4.0..40.0
        ConditioningTestType.STEP_3_MINUTE -> result in 35.0..220.0
    }
    val ageScale = conditioningAgeScale(input.ageYears)
    if (!plausible || !ageScale.isFinite()) {
        return ConditioningRankResult(null, null, true, AssessmentConfidence.LOW, listOf("The conditioning result is outside the supported range."))
    }

    // Normalize onto an adult-male reference before interpolation. Distances are
    // multiplied; times and recovery heart rate are divided because lower is better.
    val sexScale = if (input.sex == "female") .86 else 1.0
    val normalized = when (input.testType) {
        ConditioningTestType.COOPER_12_MINUTE -> result / (sexScale * ageScale)
        ConditioningTestType.RUN_1_5_MILE -> result * (sexScale * ageScale)
        ConditioningTestType.STEP_3_MINUTE -> result * (sexScale * ageScale)
    }
    val raw = when (input.testType) {
        ConditioningTestType.COOPER_12_MINUTE -> interpolate(cooperReference, normalized)
        ConditioningTestType.RUN_1_5_MILE -> interpolate(runMinutesReference, normalized)
        ConditioningTestType.STEP_3_MINUTE -> interpolate(recoveryHeartRateReference, normalized)
    }
    val score = clampScore(raw)
    val stale = todayEpochDay != null && (input.assessedAtEpochDay == null || todayEpochDay - input.assessedAtEpochDay > RankingV2Config.CONDITIONING_VALID_DAYS)
    val confidence = when {
        stale -> AssessmentConfidence.LOW
        input.assessedAtEpochDay != null -> AssessmentConfidence.HIGH
        else -> AssessmentConfidence.MEDIUM
    }
    return ConditioningRankResult(
        score = score,
        rank = scoreToRank(score),
        provisional = stale,
        confidence = confidence,
        reasons = if (stale) listOf("Conditioning assessment update recommended.") else emptyList(),
    )
}

fun computeConditioningScore(input: ConditioningInput?): Double? = computeConditioningRank(input).score

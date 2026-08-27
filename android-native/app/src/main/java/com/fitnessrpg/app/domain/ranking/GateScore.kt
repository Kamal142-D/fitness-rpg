package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank

/**
 * Gate Score -> Gate Clear Rank (PLAN.txt §6.5). Computed AFTER a workout; the
 * post-training measure of session quality (never confused with Gate Difficulty,
 * which is chosen before). Every component and the result are clamped 0..100.
 *
 * V3 never fills missing baseline or PR evidence with neutral points. That
 * missing evidence instead keeps the clear grade conservative/provisional.
 */
data class GateScoreInput(
    /** Avg performance grade score across exercises (0..100). */
    val performance: Double?,
    /** Completed working sets / planned sets, as 0..100. */
    val completion: Double?,
    /** Progress vs prior sessions (0..100), or null when no history. */
    val progress: Double?,
    /** PR bonus (0..100). */
    val pr: Double?,
    /** Training quality / RPE validity (0..100). */
    val quality: Double?,
)

fun computeGateScore(input: GateScoreInput): Double = clampScore(
    clampScore(input.performance ?: 0.0) * GateWeights.PERFORMANCE +
        clampScore(input.completion ?: 0.0) * GateWeights.COMPLETION +
        clampScore(input.progress ?: 0.0) * GateWeights.PROGRESS +
        clampScore(input.pr ?: 0.0) * GateWeights.PR,
)

fun validatedGateClearRank(
    score: Double,
    hasReliableBaseline: Boolean,
    meaningfulPrCount: Int,
    completion: Double,
    targetPerformance: Double,
    progress: Double?,
): Rank {
    var rank = scoreToRank(score)
    fun cap(max: Rank) { if (rank.ordinal > max.ordinal) rank = max }
    if (!hasReliableBaseline) cap(Rank.B)
    if (rank.ordinal >= Rank.S.ordinal && meaningfulPrCount == 0) cap(Rank.A)
    if (rank.ordinal >= Rank.S_PLUS.ordinal && meaningfulPrCount < 2) cap(Rank.S)
    if (rank.ordinal >= Rank.SS.ordinal && (meaningfulPrCount < 2 || (progress ?: 0.0) < 90.0)) cap(Rank.S_PLUS)
    if (rank == Rank.SSS && (meaningfulPrCount < 3 || completion < 95.0 || targetPerformance < 95.0 || (progress ?: 0.0) < 97.0)) cap(Rank.SS)
    return rank
}

/** Completion as a percentage of planned working sets. */
fun completionScore(completedWorkingSets: Int, plannedSets: Int): Double {
    if (plannedSets <= 0) return if (completedWorkingSets > 0) 100.0 else 0.0
    return clampScore((completedWorkingSets.toDouble() / plannedSets) * 100.0)
}

/** Progress vs a prior session's volume. Null when there is no prior history. */
fun progressScore(currentVolumeKg: Double, priorVolumeKg: Double?): Double? {
    if (priorVolumeKg == null || priorVolumeKg <= 0.0) return null
    return ratioToScore(currentVolumeKg / priorVolumeKg)
}

/** PR bonus is a real bonus. No PR means zero bonus, never a free 50 points. */
fun prComponentScore(prCount: Int): Double = when {
    prCount <= 0 -> 0.0
    prCount == 1 -> 60.0
    prCount == 2 -> 82.0
    else -> 100.0
}

/**
 * Quality from RPE validity: the fraction of working sets carrying a plausible
 * RPE. No RPE logged at all is neutral (not a penalty).
 */
fun qualityScore(rpes: List<Double?>): Double {
    if (rpes.isEmpty()) return NEUTRAL_SCORE
    val present = rpes.filterNotNull()
    if (present.isEmpty()) return NEUTRAL_SCORE
    val valid = present.count { it in 0.0..10.0 }
    return clampScore((valid.toDouble() / present.size) * 100.0)
}

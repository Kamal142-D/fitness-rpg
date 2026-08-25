package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank

/**
 * Gate Score -> Gate Clear Rank (PLAN.txt §6.5). Computed AFTER a workout; the
 * post-training measure of session quality (never confused with Gate Difficulty,
 * which is chosen before). Every component and the result are clamped 0..100.
 *
 * Missing factors are renormalized out rather than scored as zero, so new users
 * are not punished for absent comparison data.
 */
data class WeightedComponent(val value: Double?, val weight: Double)

/** Weighted average over the non-null components, renormalizing their weights. */
fun weightedRenormalized(components: List<WeightedComponent>): Double {
    var sumW = 0.0
    var sum = 0.0
    for (c in components) {
        val value = c.value ?: continue
        sumW += c.weight
        sum += clampScore(value) * c.weight
    }
    if (sumW == 0.0) return NEUTRAL_SCORE
    return clampScore(sum / sumW)
}

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

fun computeGateScore(input: GateScoreInput): Double = weightedRenormalized(
    listOf(
        WeightedComponent(input.performance, GateWeights.PERFORMANCE),
        WeightedComponent(input.completion, GateWeights.COMPLETION),
        WeightedComponent(input.progress, GateWeights.PROGRESS),
        WeightedComponent(input.pr, GateWeights.PR),
        WeightedComponent(input.quality, GateWeights.QUALITY),
    ),
)

fun gateClearRank(score: Double): Rank = scoreToRank(score)

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

/** PR bonus: absent PRs are neutral (bonus, not a penalty). */
fun prComponentScore(prCount: Int): Double = when {
    prCount <= 0 -> 50.0
    prCount == 1 -> 72.0
    prCount == 2 -> 86.0
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

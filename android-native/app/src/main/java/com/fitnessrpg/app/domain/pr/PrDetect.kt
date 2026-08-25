package com.fitnessrpg.app.domain.pr

import com.fitnessrpg.app.util.round2
import kotlin.math.max

/**
 * Personal-record detection (PLAN.txt §6, Phase 8). Pure and deterministic.
 *
 * - Only completed WORKING sets count (warm-ups never set records).
 * - Four record types; at most ONE PR per type per exercise (the best set).
 * - First-ever attempt establishes a BASELINE, not a PR (anti-spam).
 * - [DetectResult.stats] always reports the new all-time bests.
 */
private const val EPS = 0.01

private fun maxN(a: Double?, b: Double?): Double? = when {
    a == null -> b
    b == null -> a
    else -> max(a, b)
}

/** A value is a PR only if there was a prior best AND it strictly beats it. */
private fun isPR(newValue: Double, prior: Double?): Boolean {
    if (prior == null) return false // first time: baseline, not a PR
    return newValue > prior + EPS
}

fun detectPRs(
    exercises: List<DetectExercise>,
    priorStats: Map<String, PriorStat?>,
): DetectResult {
    val prs = mutableListOf<DetectedPR>()
    val stats = mutableListOf<NewStat>()

    for (ex in exercises) {
        val working = ex.sets.filter { !it.isWarmup }
        if (working.isEmpty()) continue

        var bestWeightSet: DetectSet? = null
        var bestRepsSet: DetectSet? = null
        var best1rmSet: DetectSet? = null
        var volume = 0.0

        for (s in working) {
            val w = s.weightKg
            if (w != null && w > 0.0) {
                if (bestWeightSet == null || w > (bestWeightSet.weightKg ?: 0.0)) bestWeightSet = s
            }
            val r = s.reps
            if (r != null && r > 0) {
                if (bestRepsSet == null || r > (bestRepsSet.reps ?: 0)) bestRepsSet = s
            }
            val e = s.est1RM
            if (e != null) {
                if (best1rmSet == null || e > (best1rmSet.est1RM ?: 0.0)) best1rmSet = s
            }
            volume += (s.weightKg ?: 0.0) * (s.reps ?: 0)
        }

        val prior = priorStats[ex.exerciseId]
        val sessWeight = bestWeightSet?.weightKg
        val sessReps = bestRepsSet?.reps
        val sess1rm = best1rmSet?.est1RM?.let { round2(it) }
        val sessVolume = if (volume > 0.0) round2(volume) else null

        if (sessWeight != null && isPR(sessWeight, prior?.bestWeightKg)) {
            prs.add(
                DetectedPR(
                    ex.exerciseId, ex.orderIndex, bestWeightSet!!.setNumber,
                    RecordType.WEIGHT, prior?.bestWeightKg, sessWeight,
                ),
            )
        }
        if (sessReps != null && isPR(sessReps.toDouble(), prior?.bestReps)) {
            prs.add(
                DetectedPR(
                    ex.exerciseId, ex.orderIndex, bestRepsSet!!.setNumber,
                    RecordType.REPS, prior?.bestReps, sessReps.toDouble(),
                ),
            )
        }
        if (sess1rm != null && isPR(sess1rm, prior?.bestEstimated1rmKg)) {
            prs.add(
                DetectedPR(
                    ex.exerciseId, ex.orderIndex, best1rmSet!!.setNumber,
                    RecordType.ESTIMATED_1RM, prior?.bestEstimated1rmKg, sess1rm,
                ),
            )
        }
        if (sessVolume != null && isPR(sessVolume, prior?.bestVolumeKg)) {
            prs.add(
                DetectedPR(
                    ex.exerciseId, ex.orderIndex, working.last().setNumber,
                    RecordType.VOLUME, prior?.bestVolumeKg, sessVolume,
                ),
            )
        }

        stats.add(
            NewStat(
                exerciseId = ex.exerciseId,
                bestWeightKg = maxN(prior?.bestWeightKg, sessWeight),
                bestReps = maxN(prior?.bestReps, sessReps?.toDouble()),
                bestEstimated1rmKg = maxN(prior?.bestEstimated1rmKg, sess1rm),
                bestVolumeKg = maxN(prior?.bestVolumeKg, sessVolume),
            ),
        )
    }

    return DetectResult(prs, stats)
}

private val PRIORITY: Map<RecordType, Int> = mapOf(
    RecordType.ESTIMATED_1RM to 0, // most meaningful strength progress first
    RecordType.WEIGHT to 1,
    RecordType.REPS to 2,
    RecordType.VOLUME to 3,
)

/**
 * Order PRs by importance (estimated-1RM first) and, within a type, by the size
 * of the improvement. Optionally cap the count for display. Stable sort.
 */
fun prioritizePRs(prs: List<DetectedPR>, limit: Int? = null): List<DetectedPR> {
    fun improvement(p: DetectedPR): Double =
        if (p.previousValue == null) p.newValue else p.newValue - p.previousValue
    val sorted = prs.sortedWith(
        compareBy<DetectedPR> { PRIORITY.getValue(it.recordType) }
            .thenByDescending { improvement(it) },
    )
    return if (limit == null) sorted else sorted.take(limit)
}

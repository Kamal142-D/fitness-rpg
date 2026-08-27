package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.clampScore

/**
 * Workout Performance Grade (PLAN.txt §6.4): how the user performed TODAY versus
 * their expected recent performance. Distinct from permanent Exercise Rank.
 */

/** today/baseline ratio -> 0..100 performance score. PROVISIONAL. */
private val RATIO_ANCHORS: List<Anchor> = listOf(
    Anchor(0.8, 20.0),
    Anchor(0.9, 40.0),
    Anchor(0.95, 52.0),
    Anchor(1.0, 65.0),
    Anchor(1.05, 78.0),
    Anchor(1.1, 88.0),
    Anchor(1.2, 100.0),
)

/**
 * Score today's effort against a recent baseline. Missing baseline is unranked,
 * represented as zero here; callers must show Baseline x/3 rather than a grade.
 */
fun performanceScore(todayBest: Double?, baseline: Double?): Double {
    if (todayBest == null || todayBest <= 0.0) return 0.0
    if (baseline == null || baseline <= 0.0) return 0.0
    return clampScore(interpolate(RATIO_ANCHORS, todayBest / baseline))
}

fun todayPerformanceLabel(todayBest: Double?, baseline: Double?, isPr: Boolean = false): String {
    if (isPr) return "PR"
    if (todayBest == null || baseline == null || baseline <= 0.0) return "Baseline"
    val ratio = todayBest / baseline
    return when {
        ratio < .90 -> "Below Baseline"
        ratio < 1.05 -> "Normal"
        ratio < 1.12 -> "Strong"
        else -> "Excellent"
    }
}

/** Exposed for reuse by progress scoring (same ratio curve). */
fun ratioToScore(ratio: Double): Double = clampScore(interpolate(RATIO_ANCHORS, ratio))

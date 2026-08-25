package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank

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
 * Score today's effort against a recent baseline. Missing baseline (first time)
 * or no valid effort today yields the neutral score — new users aren't punished.
 */
fun performanceScore(todayBest: Double?, baseline: Double?): Double {
    if (todayBest == null || todayBest <= 0.0) return NEUTRAL_SCORE
    if (baseline == null || baseline <= 0.0) return NEUTRAL_SCORE
    return clampScore(interpolate(RATIO_ANCHORS, todayBest / baseline))
}

fun performanceGrade(score: Double): Rank = scoreToRank(score)

/** Exposed for reuse by progress scoring (same ratio curve). */
fun ratioToScore(ratio: Double): Double = clampScore(interpolate(RATIO_ANCHORS, ratio))

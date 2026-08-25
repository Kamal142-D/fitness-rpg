package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank

/**
 * Permanent Exercise Rank (PLAN.txt §6.3) + anti-inflation upgrade guard (§6.6).
 *
 * Pipeline: best valid estimated-1RM -> bodyweight-relative ratio -> provisional
 * reference comparison -> normalized 0..100 score -> Exercise Rank.
 */
data class ExerciseScoreInput(
    val exerciseName: String,
    val bestEstimated1rmKg: Double?,
    val bodyweightKg: Double?,
    val sex: String?,
)

enum class ExerciseRankingKind { BENCHMARK_RANK, PERSONAL_PERFORMANCE_TIER }
data class ExerciseRankingResult(val kind: ExerciseRankingKind, val score: Double?, val rank: Rank?, val globallyComparable: Boolean)

fun calculatePersonalExerciseTier(currentEstimated1rmKg: Double?, historyEstimated1rmKg: List<Double>): ExerciseRankingResult {
    if (currentEstimated1rmKg == null || currentEstimated1rmKg <= 0.0 || historyEstimated1rmKg.isEmpty())
        return ExerciseRankingResult(ExerciseRankingKind.PERSONAL_PERFORMANCE_TIER, null, null, false)
    val baseline = historyEstimated1rmKg.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: return ExerciseRankingResult(ExerciseRankingKind.PERSONAL_PERFORMANCE_TIER, null, null, false)
    val score = ratioToScore(currentEstimated1rmKg / baseline)
    return ExerciseRankingResult(ExerciseRankingKind.PERSONAL_PERFORMANCE_TIER, score, scoreToRank(score), false)
}

fun calculateExerciseRanking(input: ExerciseScoreInput, personalHistory: List<Double> = emptyList()): ExerciseRankingResult {
    val benchmark = exerciseScore(input)
    return if (benchmark != null) ExerciseRankingResult(ExerciseRankingKind.BENCHMARK_RANK, benchmark, scoreToRank(benchmark), true)
    else calculatePersonalExerciseTier(input.bestEstimated1rmKg, personalHistory)
}

/**
 * Normalized 0..100 capability score for an exercise, or null when it can't be
 * scored (no strength standard for the movement, or missing bodyweight / 1RM).
 */
fun exerciseScore(input: ExerciseScoreInput): Double? {
    val movement = movementForExercise(input.exerciseName) ?: return null
    val orm = input.bestEstimated1rmKg
    val bodyweight = input.bodyweightKg
    if (orm == null || orm <= 0.0 || bodyweight == null || bodyweight <= 0.0) {
        return null
    }
    val ratio = orm / bodyweight
    return clampScore(interpolate(strengthAnchors(movement, input.sex), ratio))
}

fun permanentExerciseRank(score: Double): Rank = scoreToRank(score)

/**
 * Apply the new capability score to a prior rank with anti-inflation rules:
 * - Permanent rank is a high-water mark (never decreases on a worse session).
 * - A single update may not jump more than [ValidationLimits.MAX_RANK_JUMP] bands.
 * - Reaching S requires [ValidationLimits.MIN_SESSIONS_FOR_S] qualifying sessions.
 */
fun nextExerciseRank(prev: Rank?, candidateScore: Double, qualifyingSessions: Int): Rank {
    val ranks = Rank.entries
    val prevIdx = prev?.ordinal ?: -1
    var candIdx = scoreToRank(candidateScore).ordinal

    if (prevIdx >= 0) {
        candIdx = minOf(candIdx, prevIdx + ValidationLimits.MAX_RANK_JUMP)
    }
    if (ranks[candIdx] == Rank.S && qualifyingSessions < ValidationLimits.MIN_SESSIONS_FOR_S) {
        candIdx = Rank.A.ordinal
    }
    return ranks[maxOf(prevIdx, candIdx)]
}

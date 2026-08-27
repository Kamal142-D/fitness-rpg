package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.domain.rank.scoreToRp
import com.fitnessrpg.app.domain.rankings.Equipment
import com.fitnessrpg.app.domain.rankings.DumbbellWeightMode
import com.fitnessrpg.app.domain.rankings.ExerciseRankingMode
import com.fitnessrpg.app.domain.rankings.RankingV3Config
import com.fitnessrpg.app.domain.rankings.scoreStrengthFromEstimated1RM

data class ExerciseScoreInput(
    val exerciseName: String,
    val bestEstimated1rmKg: Double?,
    val bodyweightKg: Double?,
    val sex: String?,
    val equipment: Equipment? = null,
    val variation: String = "standard",
    val dumbbellWeightMode: DumbbellWeightMode? = null,
)

data class ExerciseRankingResult(
    val mode: ExerciseRankingMode,
    val score: Double?,
    val rank: Rank?,
    val rp: Int?,
    val globallyComparable: Boolean,
    val baselineSessions: Int,
    val requiredBaselineSessions: Int = RankingV3Config.PERSONAL_BASELINE_SESSIONS,
    val todayRpDelta: Int = 0,
    val rankChanged: Boolean = false,
    val reasons: List<String> = emptyList(),
)

private fun median(values: List<Double>): Double? {
    val sorted = values.filter { it.isFinite() && it > 0.0 }.sorted()
    if (sorted.isEmpty()) return null
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
}

/** Personal tiers require three valid baseline sessions and repeated validation for high tiers. */
fun calculatePersonalExerciseTier(
    currentEstimated1rmKg: Double?,
    historyEstimated1rmKg: List<Double>,
    previousScore: Double? = null,
): ExerciseRankingResult {
    val validHistory = historyEstimated1rmKg.filter { it.isFinite() && it > 0.0 }
    if (currentEstimated1rmKg == null || currentEstimated1rmKg <= 0.0) {
        return ExerciseRankingResult(ExerciseRankingMode.UNRANKED, null, null, null, false, validHistory.size, reasons = listOf("No valid working performance was recorded."))
    }
    val evidence = validHistory + currentEstimated1rmKg
    if (evidence.size < RankingV3Config.PERSONAL_BASELINE_SESSIONS) {
        return ExerciseRankingResult(ExerciseRankingMode.UNRANKED, null, null, null, false, evidence.size, reasons = listOf("Personal baseline requires three valid sessions."))
    }

    val baseline = median(evidence.take(RankingV3Config.PERSONAL_BASELINE_SESSIONS))
        ?: return ExerciseRankingResult(ExerciseRankingMode.UNRANKED, null, null, null, false, evidence.size)
    val rawScore = interpolate(RankingV3Config.personalProgressAnchors, currentEstimated1rmKg / baseline)
    val candidate = scoreToRank(rawScore)
    val validatingSessions = (evidence.size - RankingV3Config.PERSONAL_BASELINE_SESSIONS).coerceAtLeast(0)
    val cap = when {
        validatingSessions == 0 -> Rank.C
        validatingSessions == 1 -> Rank.C
        validatingSessions == 2 -> Rank.S
        validatingSessions == 3 -> Rank.S_PLUS
        validatingSessions in 4..5 -> Rank.SS
        else -> Rank.SSS
    }
    val rank = if (candidate.ordinal > cap.ordinal) cap else candidate
    val rp = if (rank == candidate) scoreToRp(rawScore) else 99
    val previousRank = previousScore?.let(::scoreToRank)
    val previousRp = previousScore?.let(::scoreToRp)
    val delta = if (previousRank == rank && previousRp != null) (rp - previousRp).coerceAtLeast(0) else 0
    return ExerciseRankingResult(
        mode = ExerciseRankingMode.PERSONAL,
        score = rawScore.coerceIn(0.0, 100.0),
        rank = rank,
        rp = rp,
        globallyComparable = false,
        baselineSessions = evidence.size,
        todayRpDelta = delta,
        rankChanged = previousRank != null && previousRank != rank,
        reasons = if (rank != candidate) listOf("High Personal Tiers require repeated validating sessions.") else emptyList(),
    )
}

fun calculateExerciseRanking(input: ExerciseScoreInput, personalHistory: List<Double> = emptyList()): ExerciseRankingResult {
    val benchmark = exerciseScore(input)
    if (benchmark != null) {
        return ExerciseRankingResult(
            mode = ExerciseRankingMode.GLOBAL,
            score = benchmark,
            rank = scoreToRank(benchmark),
            rp = scoreToRp(benchmark),
            globallyComparable = true,
            baselineSessions = personalHistory.size,
        )
    }
    return calculatePersonalExerciseTier(input.bestEstimated1rmKg, personalHistory)
}

fun exerciseScore(input: ExerciseScoreInput): Double? {
    var orm = input.bestEstimated1rmKg ?: return null
    val bodyweight = input.bodyweightKg ?: return null
    if (orm <= 0.0 || bodyweight <= 0.0) return null
    if (input.equipment == Equipment.DUMBBELL) {
        val mode = input.dumbbellWeightMode ?: return null
        if (mode == DumbbellWeightMode.PER_HAND) orm *= 2.0
    }
    return scoreStrengthFromEstimated1RM(
        exerciseId = input.exerciseName,
        estimated1RMkg = orm,
        bodyweightKg = bodyweight,
        sex = input.sex,
        equipment = input.equipment,
        variation = input.variation,
    )
}

fun permanentExerciseRank(score: Double): Rank = scoreToRank(score)

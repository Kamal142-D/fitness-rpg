package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.pr.PriorStat
import com.fitnessrpg.app.domain.pr.RecordType
import com.fitnessrpg.app.domain.progression.WorkoutXpInput
import com.fitnessrpg.app.domain.progression.xpForWorkout
import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.ranking.GateScoreInput
import com.fitnessrpg.app.domain.ranking.completionScore
import com.fitnessrpg.app.domain.ranking.computeGateScore
import com.fitnessrpg.app.domain.ranking.gateClearRank
import com.fitnessrpg.app.domain.ranking.performanceGrade
import com.fitnessrpg.app.domain.ranking.performanceScore
import com.fitnessrpg.app.domain.ranking.prComponentScore
import com.fitnessrpg.app.domain.ranking.qualityScore

/**
 * Compute the post-workout Gate result: Gate Score -> Gate Clear Rank,
 * per-exercise performance grades, and XP. Pure — composes the ranking engine
 * and the XP economy.
 */
data class PerExerciseResult(
    val exerciseId: String,
    val performanceScore: Double,
    val performanceGrade: Rank,
)

data class GateResult(
    val gateScore: Double,
    val gateClearRank: Rank,
    val completionScore: Double,
    val performanceScore: Double,
    val qualityScore: Double,
    val progressScore: Double?,
    val xpEarned: Int,
    val perExercise: List<PerExerciseResult>,
)

fun computeGateResult(
    payload: CompletionPayload,
    priorStats: Map<String, PriorStat?>,
    aggregates: CompletionAggregates,
    prRecordTypes: List<RecordType>,
): GateResult {
    val perExercise = mutableListOf<PerExerciseResult>()
    val rpes = mutableListOf<Double?>()

    for (ex in payload.exercises) {
        val working = ex.sets.filter { !it.isWarmup }
        for (s in working) rpes.add(s.rpe)

        val todayBest = working.mapNotNull { it.estimated1rmKg }.maxOrNull()
        val baseline = priorStats[ex.exerciseId]?.bestEstimated1rmKg
        val pScore = performanceScore(todayBest, baseline)
        perExercise.add(PerExerciseResult(ex.exerciseId, pScore, performanceGrade(pScore)))
    }

    val performanceAvg =
        if (perExercise.isNotEmpty()) perExercise.sumOf { it.performanceScore } / perExercise.size
        else null

    val completion = completionScore(aggregates.completedSets, aggregates.plannedWorkingSets)
    val quality = qualityScore(rpes)
    // Progress vs recent sessions needs history; deferred (renormalized out).
    val progress: Double? = null
    val pr = prComponentScore(prRecordTypes.size)

    val gateScore = computeGateScore(
        GateScoreInput(
            performance = performanceAvg,
            completion = completion,
            progress = progress,
            pr = pr,
            quality = quality,
        ),
    )
    val rank = gateClearRank(gateScore)

    val meaningfulPrCount = prRecordTypes.count {
        it == RecordType.ESTIMATED_1RM || it == RecordType.WEIGHT
    }
    val xpEarned = xpForWorkout(
        WorkoutXpInput(
            completed = true,
            validWorkingSets = aggregates.completedSets,
            meaningfulPrCount = meaningfulPrCount,
            gateClearRank = rank,
        ),
    )

    return GateResult(
        gateScore = gateScore,
        gateClearRank = rank,
        completionScore = completion,
        performanceScore = performanceAvg ?: 60.0,
        qualityScore = quality,
        progressScore = progress,
        xpEarned = xpEarned,
        perExercise = perExercise,
    )
}

package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.gates.GateDifficultyResult
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.pr.PriorStat
import com.fitnessrpg.app.domain.pr.RecordType
import com.fitnessrpg.app.domain.progression.WorkoutXpInput
import com.fitnessrpg.app.domain.progression.xpForWorkout
import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.ranking.ExerciseScoreInput
import com.fitnessrpg.app.domain.ranking.GateScoreInput
import com.fitnessrpg.app.domain.ranking.calculateExerciseRanking
import com.fitnessrpg.app.domain.ranking.completionScore
import com.fitnessrpg.app.domain.ranking.computeGateScore
import com.fitnessrpg.app.domain.ranking.performanceScore
import com.fitnessrpg.app.domain.ranking.prComponentScore
import com.fitnessrpg.app.domain.ranking.progressScore
import com.fitnessrpg.app.domain.ranking.qualityScore
import com.fitnessrpg.app.domain.ranking.todayPerformanceLabel
import com.fitnessrpg.app.domain.ranking.validatedGateClearRank
import com.fitnessrpg.app.domain.rankings.Equipment
import com.fitnessrpg.app.domain.rankings.ExerciseRankingMode
import kotlin.math.abs

data class PerExerciseResult(
    val exerciseId: String,
    val rankingMode: ExerciseRankingMode,
    /** Continuous permanent Global Rank / Personal Tier score. */
    val exerciseScore: Double?,
    val exerciseRank: Rank?,
    val exerciseRp: Int?,
    val previousRank: Rank? = null,
    val previousRp: Int? = null,
    val rpDelta: Int = 0,
    val rankChanged: Boolean = false,
    val baselineSessions: Int = 0,
    val requiredBaselineSessions: Int = 3,
    val todayLabel: String = "Baseline",
    /** Today's target execution score. This is not an Exercise Rank. */
    val performanceScore: Double = 0.0,
    /** Retained only for source compatibility; V3 never displays or stores it. */
    val performanceGrade: Rank? = null,
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
    val difficulty: GateDifficultyResult? = null,
    val clearProvisional: Boolean = false,
    val exercisesGainedRp: Int = 0,
    val exercisesUnchanged: Int = 0,
    val rankUps: Int = 0,
)

private fun equipmentOf(value: String?): Equipment = when (value?.trim()?.lowercase()) {
    "barbell" -> Equipment.BARBELL
    "dumbbell", "dumbbells" -> Equipment.DUMBBELL
    "machine" -> Equipment.MACHINE
    "smith machine", "smith_machine", "smith" -> Equipment.SMITH_MACHINE
    "cable", "cables" -> Equipment.CABLE
    "body weight", "bodyweight", "weighted" -> Equipment.BODYWEIGHT
    else -> Equipment.OTHER
}

private fun bestEstimated1rm(exercise: CompletionExercisePayload, equipment: Equipment, bodyweightKg: Double?): Double? =
    exercise.sets.asSequence()
        .filter { !it.isWarmup }
        .mapNotNull { set ->
            if (equipment == Equipment.BODYWEIGHT && bodyweightKg != null && (set.reps ?: 0) in 1..12) {
                (bodyweightKg + (set.weightKg ?: 0.0)).takeIf { it > 0.0 }
                    ?.let { it * (1.0 + (set.reps ?: 0) / 30.0) }
            } else {
                set.estimated1rmKg?.takeIf { value -> value.isFinite() && value > 0.0 }
            }
        }
        .maxOrNull()

private fun targetExecutionScore(exercise: CompletionExercisePayload): Double {
    val working = exercise.sets.filter { !it.isWarmup }
    if (working.isEmpty()) return 0.0
    val scored = working.map { set ->
        val repsScore = when {
            set.reps == null || exercise.targetRepsMin == null || exercise.targetRepsMax == null -> 100.0
            set.reps in exercise.targetRepsMin..exercise.targetRepsMax -> 100.0
            else -> (100.0 - abs(set.reps - set.reps.coerceIn(exercise.targetRepsMin, exercise.targetRepsMax)) * 12.5).coerceAtLeast(0.0)
        }
        val rpeScore = when {
            set.rpe == null || exercise.targetRpe == null -> 100.0
            else -> (100.0 - abs(set.rpe - exercise.targetRpe) * 20.0).coerceAtLeast(0.0)
        }
        repsScore * 0.65 + rpeScore * 0.35
    }
    return clampScore(scored.average())
}

private fun ordinalRp(rank: Rank?, rp: Int?): Int? =
    if (rank == null || rp == null) null else rank.ordinal * 100 + rp

/**
 * Produces the V3 completion result. Permanent exercise ranks come only from
 * standardized strength standards or a three-session personal baseline. Gate
 * difficulty and Gate clear grade remain separate outputs.
 */
fun computeGateResult(
    payload: CompletionPayload,
    priorStats: Map<String, PriorStat>,
    aggregates: CompletionAggregates,
    prRecordTypes: List<RecordType>,
    difficulty: GateDifficultyResult? = null,
    priorSessionVolumeKg: Double? = null,
    exerciseMetadata: Map<String, Exercise> = emptyMap(),
    exerciseHistory: Map<String, List<Double>> = emptyMap(),
    bodyweightKg: Double? = null,
    sex: String? = null,
): GateResult {
    val perExercise = payload.exercises.map { exercise ->
        val metadata = exerciseMetadata[exercise.exerciseId]
        val equipment = equipmentOf(metadata?.equipment)
        val todayBest = bestEstimated1rm(exercise, equipment, bodyweightKg)
        val history = exerciseHistory[exercise.exerciseId].orEmpty().filter { it.isFinite() && it > 0.0 }
        val rankingInput = ExerciseScoreInput(
            exerciseName = metadata?.name ?: exercise.exerciseId,
            bestEstimated1rmKg = todayBest,
            bodyweightKg = bodyweightKg,
            sex = sex,
            equipment = equipment,
        )
        val current = calculateExerciseRanking(rankingInput, history)
        val previous = when {
            history.isNotEmpty() -> calculateExerciseRanking(
                rankingInput.copy(bestEstimated1rmKg = history.last()),
                history.dropLast(1),
            )
            else -> null
        }
        val currentOrdinalRp = ordinalRp(current.rank, current.rp)
        val previousOrdinalRp = ordinalRp(previous?.rank, previous?.rp)
        val delta = if (currentOrdinalRp != null && previousOrdinalRp != null) {
            (currentOrdinalRp - previousOrdinalRp).coerceAtLeast(0)
        } else 0
        val oldBest = history.maxOrNull() ?: priorStats[exercise.exerciseId]?.bestEstimated1rmKg
        PerExerciseResult(
            exerciseId = exercise.exerciseId,
            rankingMode = current.mode,
            exerciseScore = current.score,
            exerciseRank = current.rank,
            exerciseRp = current.rp,
            previousRank = previous?.rank,
            previousRp = previous?.rp,
            rpDelta = delta,
            rankChanged = current.rank != null && previous != null && current.rank != previous.rank,
            baselineSessions = current.baselineSessions,
            requiredBaselineSessions = current.requiredBaselineSessions,
            todayLabel = todayPerformanceLabel(todayBest, oldBest, oldBest != null && todayBest != null && todayBest > oldBest),
            performanceScore = targetExecutionScore(exercise),
        )
    }

    val targetPerformance = perExercise.map { it.performanceScore }.averageOrZero()
    val completion = completionScore(aggregates.completedSets, aggregates.plannedWorkingSets)
    val progress = progressScore(aggregates.totalVolumeKg, priorSessionVolumeKg)
    val meaningfulPrCount = prRecordTypes.count { it == RecordType.ESTIMATED_1RM || it == RecordType.WEIGHT }
    val gateScore = computeGateScore(
        GateScoreInput(
            performance = targetPerformance,
            completion = completion,
            progress = progress,
            pr = prComponentScore(meaningfulPrCount),
            quality = null,
        ),
    )
    val personalEvidenceReady = perExercise
        .filter { it.rankingMode != ExerciseRankingMode.GLOBAL }
        .all { it.baselineSessions >= it.requiredBaselineSessions }
    val reliableBaseline = priorSessionVolumeKg != null && priorSessionVolumeKg > 0.0 && personalEvidenceReady
    val clearRank = validatedGateClearRank(
        score = gateScore,
        hasReliableBaseline = reliableBaseline,
        meaningfulPrCount = meaningfulPrCount,
        completion = completion,
        targetPerformance = targetPerformance,
        progress = progress,
    )
    val validWorkingSets = payload.exercises.sumOf { exercise ->
        exercise.sets.count { !it.isWarmup && (it.reps ?: 0) > 0 }
    }
    val quality = qualityScore(payload.exercises.flatMap { exercise -> exercise.sets.filter { !it.isWarmup }.map { it.rpe } })
    return GateResult(
        gateScore = gateScore,
        gateClearRank = clearRank,
        completionScore = completion,
        performanceScore = targetPerformance,
        qualityScore = quality,
        progressScore = progress,
        xpEarned = xpForWorkout(WorkoutXpInput(true, validWorkingSets, meaningfulPrCount, clearRank)),
        perExercise = perExercise,
        difficulty = difficulty,
        clearProvisional = !reliableBaseline,
        exercisesGainedRp = perExercise.count { it.rpDelta > 0 },
        exercisesUnchanged = perExercise.count { it.rankingMode != ExerciseRankingMode.UNRANKED && it.rpDelta == 0 },
        rankUps = perExercise.count { it.rankChanged },
    )
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

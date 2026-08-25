package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.scoreToRank
import kotlin.math.max

/** Tunable weights for personalized, post-workout Gate Difficulty. */
data class GateDifficultyConfig(
    val relativeIntensityWeight: Double = 0.45,
    val hardWorkingSetsWeight: Double = 0.25,
    val volumeVsBaselineWeight: Double = 0.20,
    val densityWeight: Double = 0.10,
    @Deprecated("V2 uses hardWorkingSetsWeight") val workingVolumeWeight: Double = hardWorkingSetsWeight,
    @Deprecated("V2 uses volumeVsBaselineWeight") val setDifficultyWeight: Double = volumeVsBaselineWeight,
    @Deprecated("V2 uses densityWeight") val progressionWeight: Double = densityWeight,
    val averageExerciseWeight: Double = 0.70,
    val hardestExerciseWeight: Double = 0.20,
    val workoutVolumeWeight: Double = 0.10,
)

data class DifficultySet(
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val isWarmup: Boolean,
)

data class ExerciseDifficultyInput(
    val exerciseId: String,
    val sets: List<DifficultySet>,
    val currentEstimated1rmKg: Double?,
    val currentExerciseRank: Rank? = null,
    val bodyWeightKg: Double? = null,
    val equipment: String? = null,
    val isMajorExercise: Boolean = true,
    val recentAverageVolumeKg: Double? = null,
    val priorSessionCount: Int = 0,
)

data class ExerciseDifficultyResult(
    val exerciseId: String,
    val score: Double,
    val rank: Rank,
    val workingVolumeKg: Double,
)

data class GateDifficultyResult(
    val score: Double,
    val rank: Rank,
    val perExercise: List<ExerciseDifficultyResult>,
    val provisional: Boolean = false,
    val confidence: String = "high",
)

private fun clamp(value: Double): Double = value.coerceIn(0.0, 100.0)

fun effectiveLoadKg(weightKg: Double?, bodyWeightKg: Double?, equipment: String?): Double {
    val entered = (weightKg ?: 0.0).coerceAtLeast(0.0)
    val bodyweight = equipment?.lowercase()?.let {
        it.contains("body weight") || it.contains("bodyweight") || it == "weighted"
    } == true
    return if (bodyweight) (bodyWeightKg ?: 0.0) + entered else entered
}

/** Relative load of one working set against this user's known estimated 1RM. */
fun calculateSetIntensity(
    set: DifficultySet,
    currentEstimated1rmKg: Double?,
    bodyWeightKg: Double? = null,
    equipment: String? = null,
): Double {
    if (set.isWarmup || currentEstimated1rmKg == null || currentEstimated1rmKg <= 0.0) return 0.0
    return (effectiveLoadKg(set.weightKg, bodyWeightKg, equipment) / currentEstimated1rmKg)
        .coerceIn(0.0, 1.25)
}

fun calculateSetDifficulty(set: DifficultySet, currentEstimated1rmKg: Double?, bodyWeightKg: Double? = null, equipment: String? = null): Double =
    if (set.isWarmup) 0.0 else clamp(calculateSetIntensity(set, currentEstimated1rmKg, bodyWeightKg, equipment) / 0.85 * 100.0)

/** Personalized 0–100 difficulty for one exercise. Warm-ups are excluded. */
fun calculateExerciseDifficulty(
    input: ExerciseDifficultyInput,
    config: GateDifficultyConfig = GateDifficultyConfig(),
): ExerciseDifficultyResult {
    val working = input.sets.filter { !it.isWarmup && (it.reps ?: 0) > 0 }
    if (working.isEmpty()) return ExerciseDifficultyResult(input.exerciseId, 0.0, Rank.E, 0.0)

    val loads = working.map { effectiveLoadKg(it.weightKg, input.bodyWeightKg, input.equipment) }
    val estimated = working.mapIndexedNotNull { index, set ->
        val reps = set.reps ?: return@mapIndexedNotNull null
        if (reps !in 1..12 || loads[index] <= 0.0) null else loads[index] * (1.0 + reps / 30.0)
    }
    val todayBest = estimated.maxOrNull()
    val rankFallback = input.currentExerciseRank?.let { 35.0 + it.ordinal * 12.0 }
    val baseline = input.currentEstimated1rmKg?.takeIf { it > 0.0 }
        ?: todayBest
        ?: rankFallback
        ?: max(1.0, loads.maxOrNull() ?: 1.0)

    val relativeIntensity = working.map {
        calculateSetIntensity(it, baseline, input.bodyWeightKg, input.equipment)
    }.average()
    val intensityScore = clamp(relativeIntensity / 0.85 * 100.0)

    val volume = working.indices.sumOf { loads[it] * (working[it].reps ?: 0) }
    val volumeScore = input.recentAverageVolumeKg?.takeIf { it > 0 }?.let { clamp(volume / it * 60.0) } ?: 50.0

    val effortAverage = working.map { set ->
        val repDemand = ((set.reps ?: 0).coerceIn(1, 20) / 12.0 * 65.0).coerceAtMost(100.0)
        val rpeDemand = set.rpe?.coerceIn(0.0, 10.0)?.times(10.0) ?: 60.0
        repDemand * 0.45 + rpeDemand * 0.55
    }.average()
    val setCountFactor = (working.size / 4.0).coerceIn(0.0, 1.0) * 100.0
    val setDifficultyScore = clamp(effortAverage * 0.75 + setCountFactor * 0.25)

    val score = clamp(
        intensityScore * config.relativeIntensityWeight +
            setDifficultyScore * config.hardWorkingSetsWeight +
            volumeScore * config.volumeVsBaselineWeight +
            50.0 * config.densityWeight,
    )
    return ExerciseDifficultyResult(input.exerciseId, score, scoreToRank(score), volume)
}

/** Combines exercise difficulty without allowing one hard movement to dominate. */
fun calculateGateDifficulty(
    inputs: List<ExerciseDifficultyInput>,
    config: GateDifficultyConfig = GateDifficultyConfig(),
    workoutDurationMinutes: Double? = null,
): GateDifficultyResult {
    val perExercise = inputs.map { calculateExerciseDifficulty(it, config) }.filter { it.workingVolumeKg > 0.0 }
    if (perExercise.isEmpty()) return GateDifficultyResult(0.0, Rank.E, emptyList())

    val average = perExercise.map { it.score }.average()
    val majorIds = inputs.filter { it.isMajorExercise }.map { it.exerciseId }.toSet()
    val hardestMajor = perExercise.filter { it.exerciseId in majorIds }.maxOfOrNull { it.score }
        ?: perExercise.maxOf { it.score }
    val totalVolume = perExercise.sumOf { it.workingVolumeKg }
    val baselines = inputs.mapNotNull { it.currentEstimated1rmKg?.takeIf { value -> value > 0.0 } }.sum()
    val volumeFactor = if (baselines > 0.0) clamp(totalVolume / (baselines * 30.0) * 100.0) else average

    val workingSets = inputs.sumOf { it.sets.count { set -> !set.isWarmup && (set.reps ?: 0) > 0 } }
    val densityScore = workoutDurationMinutes?.takeIf { it > 0 }?.let { clamp((workingSets / it) / 0.35 * 100.0) } ?: 50.0
    val score = clamp(average * 0.70 + hardestMajor * 0.20 + volumeFactor * 0.05 + densityScore * 0.05)
    val provisional = inputs.any { it.priorSessionCount < 3 || it.recentAverageVolumeKg == null }
    return GateDifficultyResult(score, scoreToRank(score), perExercise, provisional, if (provisional) "low" else "high")
}

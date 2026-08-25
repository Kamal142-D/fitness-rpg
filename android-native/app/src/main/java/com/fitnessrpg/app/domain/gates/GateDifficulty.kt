package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.domain.rankings.RankingV2Config

data class GateDifficultyConfig(
    val relativeIntensityWeight: Double = RankingV2Config.GATE_INTENSITY_WEIGHT,
    val hardWorkingSetsWeight: Double = RankingV2Config.GATE_HARD_SETS_WEIGHT,
    val volumeVsBaselineWeight: Double = RankingV2Config.GATE_VOLUME_WEIGHT,
    val densityWeight: Double = RankingV2Config.GATE_DENSITY_WEIGHT,
    val targetIntensity: Double = .85,
    val hardSetIntensity: Double = .65,
    val hardSetRpe: Double = 7.0,
    val hardSetsForMaxScore: Int = 10,
    val targetSetsPerMinute: Double = .35,
)

data class DifficultySet(val weightKg: Double?, val reps: Int?, val rpe: Double?, val isWarmup: Boolean)

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
    val intensityScore: Double = 0.0,
    val hardWorkingSets: Int = 0,
    val hardSetsScore: Double = 0.0,
    val volumeScore: Double = 50.0,
    val provisional: Boolean = true,
    val workingSetCount: Int = 0,
    val volumeBaselineKnown: Boolean = false,
)

data class GateDifficultyResult(
    val score: Double,
    val rank: Rank,
    val perExercise: List<ExerciseDifficultyResult>,
    val provisional: Boolean = false,
    val confidence: String = "high",
    val intensityScore: Double = 0.0,
    val hardSetsScore: Double = 0.0,
    val volumeScore: Double = 50.0,
    val densityScore: Double = 50.0,
)

private fun clamp(value: Double): Double = value.coerceIn(0.0, 100.0)

fun effectiveLoadKg(weightKg: Double?, bodyWeightKg: Double?, equipment: String?): Double {
    val entered = (weightKg ?: 0.0).coerceAtLeast(0.0)
    val bodyweight = equipment?.lowercase()?.let {
        it.contains("body weight") || it.contains("bodyweight") || it == "weighted"
    } == true
    return if (bodyweight) (bodyWeightKg ?: 0.0) + entered else entered
}

fun calculateSetIntensity(
    set: DifficultySet,
    currentEstimated1rmKg: Double?,
    bodyWeightKg: Double? = null,
    equipment: String? = null,
): Double {
    if (set.isWarmup || currentEstimated1rmKg == null || currentEstimated1rmKg <= 0.0) return 0.0
    return (effectiveLoadKg(set.weightKg, bodyWeightKg, equipment) / currentEstimated1rmKg).coerceIn(0.0, 1.25)
}

fun calculateSetDifficulty(set: DifficultySet, currentEstimated1rmKg: Double?, bodyWeightKg: Double? = null, equipment: String? = null): Double =
    if (set.isWarmup) 0.0 else clamp(calculateSetIntensity(set, currentEstimated1rmKg, bodyWeightKg, equipment) / .85 * 100.0)

private fun volumeAgainstBaselineScore(volume: Double, baseline: Double?): Double {
    if (baseline == null || baseline <= 0.0) return 50.0
    val ratio = volume / baseline
    return when {
        ratio <= 0.0 -> 0.0
        ratio < 1.0 -> ratio * 60.0
        ratio < 1.5 -> 60.0 + (ratio - 1.0) * 50.0
        else -> 85.0 + (ratio - 1.5) * 30.0
    }.let(::clamp)
}

fun calculateExerciseDifficulty(input: ExerciseDifficultyInput, config: GateDifficultyConfig = GateDifficultyConfig()): ExerciseDifficultyResult {
    val working = input.sets.filter { !it.isWarmup && (it.reps ?: 0) > 0 }
    if (working.isEmpty()) return ExerciseDifficultyResult(input.exerciseId, 0.0, Rank.E, 0.0)
    val loads = working.map { effectiveLoadKg(it.weightKg, input.bodyWeightKg, input.equipment) }
    val todayBest = working.indices.mapNotNull { i ->
        val reps = working[i].reps ?: return@mapNotNull null
        loads[i].takeIf { it > 0.0 && reps in 1..12 }?.let { it * (1.0 + reps / 30.0) }
    }.maxOrNull()
    val baseline = input.currentEstimated1rmKg?.takeIf { it > 0.0 } ?: todayBest
    val intensities = working.map { calculateSetIntensity(it, baseline, input.bodyWeightKg, input.equipment) }
    val intensityScore = clamp(intensities.average() / config.targetIntensity * 100.0)
    val hardSets = working.indices.count { i ->
        (working[i].rpe ?: 0.0) >= config.hardSetRpe || intensities[i] >= config.hardSetIntensity
    }
    val hardScore = clamp(hardSets.toDouble() / minOf(config.hardSetsForMaxScore, maxOf(1, working.size)) * 100.0)
    val volume = working.indices.sumOf { loads[it] * (working[it].reps ?: 0) }
    val volumeScore = volumeAgainstBaselineScore(volume, input.recentAverageVolumeKg)
    // Per-exercise score excludes density and renormalizes the remaining weights.
    val exerciseWeight = config.relativeIntensityWeight + config.hardWorkingSetsWeight + config.volumeVsBaselineWeight
    val score = clamp((intensityScore * config.relativeIntensityWeight + hardScore * config.hardWorkingSetsWeight + volumeScore * config.volumeVsBaselineWeight) / exerciseWeight)
    val provisional = input.currentEstimated1rmKg == null || input.recentAverageVolumeKg == null || input.priorSessionCount < 3
    return ExerciseDifficultyResult(input.exerciseId, score, scoreToRank(score), volume, intensityScore, hardSets, hardScore, volumeScore, provisional, working.size, input.recentAverageVolumeKg != null)
}

/** Exact V2 Gate formula: intensity 45%, hard sets 25%, personal volume 20%, density 10%. */
fun calculateGateDifficulty(
    inputs: List<ExerciseDifficultyInput>,
    config: GateDifficultyConfig = GateDifficultyConfig(),
    workoutDurationMinutes: Double? = null,
): GateDifficultyResult {
    val exercises = inputs.map { calculateExerciseDifficulty(it, config) }.filter { it.workingVolumeKg > 0.0 }
    if (exercises.isEmpty()) return GateDifficultyResult(0.0, Rank.E, emptyList(), true, "low")
    val totalHardSets = exercises.sumOf { it.hardWorkingSets }
    val allWorkingSets = exercises.sumOf { it.workingSetCount }
    val intensity = exercises.sumOf { it.intensityScore * it.workingSetCount } / allWorkingSets.coerceAtLeast(1)
    val hardSets = clamp(totalHardSets.toDouble() / config.hardSetsForMaxScore * 100.0)
    val knownVolume = exercises.filter { it.volumeBaselineKnown }
    val volume = (knownVolume.ifEmpty { exercises }).map { it.volumeScore }.average()
    val density = workoutDurationMinutes?.takeIf { it > 0.0 }?.let {
        clamp((allWorkingSets / it) / config.targetSetsPerMinute * 100.0)
    } ?: 50.0
    val score = clamp(
        intensity * config.relativeIntensityWeight +
            hardSets * config.hardWorkingSetsWeight +
            volume * config.volumeVsBaselineWeight +
            density * config.densityWeight,
    )
    val provisional = exercises.any { it.provisional } || workoutDurationMinutes == null
    return GateDifficultyResult(score, scoreToRank(score), exercises, provisional, if (provisional) "low" else "high", intensity, hardSets, volume, density)
}

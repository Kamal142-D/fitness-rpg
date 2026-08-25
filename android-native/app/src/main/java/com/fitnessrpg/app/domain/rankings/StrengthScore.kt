package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.domain.ranking.Anchor
import com.fitnessrpg.app.domain.ranking.interpolate

data class ExerciseBenchmarkKey(val exercise: String, val variation: String, val equipment: Equipment)
data class ExerciseStrengthStandard(
    val key: ExerciseBenchmarkKey,
    val movement: MovementPattern,
    val maleRatios: Map<Rank, Double>,
    val loadMode: LoadMode = LoadMode.EXTERNAL,
)
enum class LoadMode { EXTERNAL, BODYWEIGHT_PLUS_EXTERNAL }

private fun ratios(d: Double, c: Double, b: Double, a: Double, s: Double) = mapOf(
    Rank.E to 0.0, Rank.D to d, Rank.C to c, Rank.B to b, Rank.A to a, Rank.S to s,
)

/** Separate standards by exercise + variation + equipment. No machine conversion factors. */
val EXERCISE_STRENGTH_STANDARDS = listOf(
    ExerciseStrengthStandard(ExerciseBenchmarkKey("bench", "flat", Equipment.BARBELL), MovementPattern.HORIZONTAL_PUSH, ratios(.45,.70,1.0,1.30,1.60)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("bench", "flat", Equipment.DUMBBELL), MovementPattern.HORIZONTAL_PUSH, ratios(.35,.55,.80,1.05,1.30)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("squat", "back", Equipment.BARBELL), MovementPattern.KNEE_DOMINANT, ratios(.50,.85,1.20,1.60,2.0)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("squat", "front", Equipment.BARBELL), MovementPattern.KNEE_DOMINANT, ratios(.40,.70,1.0,1.35,1.70)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("deadlift", "conventional", Equipment.BARBELL), MovementPattern.HIP_HINGE, ratios(.75,1.10,1.50,2.0,2.50)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("ohp", "standing", Equipment.BARBELL), MovementPattern.VERTICAL_PUSH, ratios(.30,.50,.70,.90,1.15)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("ohp", "seated", Equipment.DUMBBELL), MovementPattern.VERTICAL_PUSH, ratios(.22,.38,.55,.72,.90)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("pullup", "strict", Equipment.BODYWEIGHT), MovementPattern.VERTICAL_PULL, ratios(.85,1.0,1.12,1.30,1.50), LoadMode.BODYWEIGHT_PLUS_EXTERNAL),
)

private val aliases = mapOf(
    "barbell bench press" to ("bench" to "flat"), "bench" to ("bench" to "flat"),
    "dumbbell bench press" to ("bench" to "flat"),
    "barbell back squat" to ("squat" to "back"), "squat" to ("squat" to "back"),
    "front squat" to ("squat" to "front"),
    "deadlift" to ("deadlift" to "conventional"), "barbell deadlift" to ("deadlift" to "conventional"),
    "overhead press" to ("ohp" to "standing"), "ohp" to ("ohp" to "standing"),
    "dumbbell shoulder press" to ("ohp" to "seated"),
    "pull-up" to ("pullup" to "strict"), "pull up" to ("pullup" to "strict"), "pullup" to ("pullup" to "strict"),
)

private fun benchmarkFor(input: StrengthAssessmentInput): ExerciseStrengthStandard? {
    val normalized = input.exerciseId.trim().lowercase()
    val alias = aliases[normalized] ?: return null
    val requestedVariation = input.variation.trim().lowercase().takeUnless { it == "standard" } ?: alias.second
    return EXERCISE_STRENGTH_STANDARDS.firstOrNull {
        it.key.exercise == alias.first && it.key.variation == requestedVariation && it.key.equipment == input.equipment
    }
}

fun movementForExerciseId(exerciseId: String): String? = aliases[exerciseId.trim().lowercase()]?.first

private fun sexScale(sex: String?): Double = when (sex) {
    "male" -> 1.0
    "female" -> .72
    else -> .86
}

private fun totalExternalLoad(input: StrengthAssessmentInput): Double = when {
    input.equipment == Equipment.DUMBBELL && input.dumbbellWeightMode == DumbbellWeightMode.PER_HAND -> input.weightKg * 2.0
    input.equipment == Equipment.DUMBBELL && input.dumbbellWeightMode == null -> Double.NaN
    else -> input.weightKg
}

private fun relativeToScore(relative: Double, standard: ExerciseStrengthStandard, sex: String?, ageYears: Int? = null): Double {
    val ageScale = RankingV2Config.ageBands.firstOrNull { (ageYears ?: 30) in it.range }?.strengthScale ?: 1.0
    val scale = sexScale(sex) * ageScale
    val scoreAnchors = mapOf(Rank.E to 0.0, Rank.D to 20.0, Rank.C to 35.0, Rank.B to 50.0, Rank.A to 65.0, Rank.S to 80.0)
    val anchors = buildList {
        Rank.entries.forEach { rank -> add(Anchor(standard.maleRatios.getValue(rank) * scale, scoreAnchors.getValue(rank))) }
        add(Anchor(standard.maleRatios.getValue(Rank.S) * scale * 1.25, 100.0))
    }
    return clampScore(interpolate(anchors, relative))
}

fun scoreStrengthMovement(input: StrengthAssessmentInput, bodyweightKg: Double, sex: String?, ageYears: Int? = null): Double? {
    val standard = benchmarkFor(input) ?: return null
    if (bodyweightKg !in 30.0..300.0 || input.weightKg < 0.0 || input.reps !in 1..50) return null
    val external = totalExternalLoad(input)
    if (!external.isFinite() || external < 0.0) return null
    val rankingReps = input.reps.coerceAtMost(12)
    val rankedLoad = when (standard.loadMode) {
        LoadMode.EXTERNAL -> external
        LoadMode.BODYWEIGHT_PLUS_EXTERNAL -> bodyweightKg + external
    }
    if (rankedLoad <= 0.0) return null
    val estimated1rm = calculateEstimated1RM(rankedLoad, rankingReps)
    return relativeToScore(estimated1rm / bodyweightKg, standard, sex, ageYears)
}

fun scoreStrengthFromEstimated1RM(
    exerciseId: String,
    estimated1RMkg: Double,
    bodyweightKg: Double,
    sex: String?,
    equipment: Equipment? = null,
    variation: String = "standard",
    ageYears: Int? = null,
): Double? {
    val inferredEquipment = equipment ?: when {
        exerciseId.contains("pull", true) -> Equipment.BODYWEIGHT
        exerciseId.contains("barbell", true) || exerciseId.lowercase() in setOf("bench", "squat", "deadlift", "ohp", "front squat") -> Equipment.BARBELL
        else -> return null
    }
    val input = StrengthAssessmentInput(exerciseId, inferredEquipment, estimated1RMkg, 1, variation = variation)
    val standard = benchmarkFor(input) ?: return null
    if (estimated1RMkg <= 0.0 || bodyweightKg !in 30.0..300.0) return null
    return relativeToScore(estimated1RMkg / bodyweightKg, standard, sex, ageYears)
}

private fun combineMovementScores(scores: Collection<Double>): Double? {
    if (scores.isEmpty()) return null
    return clampScore(scores.average() * RankingV2Config.STRENGTH_AVERAGE_WEIGHT + scores.min() * RankingV2Config.STRENGTH_WEAKEST_WEIGHT)
}

fun computeStrengthRank(
    inputs: List<StrengthAssessmentInput>,
    bodyweightKg: Double,
    sex: String?,
    qualifyingSessionCount: Int = 1,
    todayEpochDay: Long? = null,
    ageYears: Int? = null,
): StrengthRankResult {
    val reasons = mutableListOf<String>()
    if (inputs.isEmpty()) return StrengthRankResult(null, null, emptyMap(), Rank.C, true, AssessmentConfidence.LOW, listOf("Strength Assessment Incomplete: equipment, weight and repetitions are required."))

    if (inputs.any { it.equipment == Equipment.DUMBBELL && it.dumbbellWeightMode == null }) reasons += "Dumbbell weight must specify per hand or total."
    if (inputs.any { it.reps > 12 }) reasons += "Sets above 12 repetitions reduce strength-assessment confidence."
    if (inputs.any { benchmarkFor(it) == null }) reasons += "Non-standardized machine/cable variations use Personal Performance Tier, not global Strength Rank."

    val scored = inputs.mapNotNull { input ->
        val standard = benchmarkFor(input) ?: return@mapNotNull null
        val score = scoreStrengthMovement(input, bodyweightKg, sex, ageYears) ?: return@mapNotNull null
        Triple(standard.movement, score, input)
    }
    val movementScores = scored.groupBy { it.first }.mapValues { (_, values) -> values.maxOf { it.second } }
    val score = combineMovementScores(movementScores.values)
    val movementCount = movementScores.size
    val hasDates = scored.isNotEmpty() && scored.all { it.third.performedAtEpochDay != null }
    val recent = todayEpochDay != null && hasDates && scored.all { todayEpochDay - it.third.performedAtEpochDay!! <= RankingV2Config.STRENGTH_VALID_DAYS }
    val stale = todayEpochDay != null && !recent
    val evidenceSessions = inputs.mapNotNull { it.sessionId }.distinct().size.coerceAtLeast(qualifyingSessionCount)

    if (movementCount < 2) reasons += "At least two major movement patterns are required for B Strength."
    if (movementCount < 3) reasons += "At least three major movement patterns are required for A or S Strength."
    if (stale) reasons += "Recent strength evidence from the last 60 days is required for A or S."
    if (evidenceSessions < 2) reasons += "Repeated qualifying performance is required for A or S Strength."

    val cap = when {
        movementCount == 0 -> Rank.C
        movementCount == 1 -> Rank.C
        movementCount == 2 -> Rank.B
        stale -> Rank.B
        evidenceSessions < 2 -> Rank.B
        else -> Rank.S
    }
    var rank = score?.let(::scoreToRank)
    if (rank != null && rank.ordinal > cap.ordinal) rank = cap
    val confidence = when {
        score == null || movementCount < 2 || stale || inputs.any { it.reps > 12 } -> AssessmentConfidence.LOW
        movementCount >= 3 && evidenceSessions >= 2 && recent -> AssessmentConfidence.HIGH
        else -> AssessmentConfidence.MEDIUM
    }
    val provisional = score == null || movementCount < 2 || stale || inputs.any { it.reps > 12 }
    return StrengthRankResult(score, rank, movementScores.mapKeys { it.key.name }, cap, provisional, confidence, reasons.distinct())
}

fun computeStrengthScore(inputs: List<StrengthAssessmentInput>, bodyweightKg: Double, sex: String?): Double? =
    computeStrengthRank(inputs, bodyweightKg, sex).score

fun computeStrengthScoreFromEstimated1RMs(items: List<Pair<String, Double>>, bodyweightKg: Double, sex: String?): Double? =
    combineMovementScores(items.mapNotNull { scoreStrengthFromEstimated1RM(it.first, it.second, bodyweightKg, sex) })

package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.RANK_THRESHOLDS
import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRp
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

private fun ratios(vararg values: Double): Map<Rank, Double> = Rank.entries.zip(values.toList()).toMap()

/** Only standardized movements belong here. Machine and cable loads are excluded. */
val EXERCISE_STRENGTH_STANDARDS = listOf(
    ExerciseStrengthStandard(ExerciseBenchmarkKey("bench", "flat", Equipment.BARBELL), MovementPattern.HORIZONTAL_PUSH, ratios(0.0,.45,.70,1.0,1.30,1.60,1.80,2.0,2.20)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("bench", "flat", Equipment.DUMBBELL), MovementPattern.HORIZONTAL_PUSH, ratios(0.0,.35,.55,.80,1.05,1.30,1.48,1.65,1.85)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("row", "bent_over", Equipment.BARBELL), MovementPattern.HORIZONTAL_PULL, ratios(0.0,.40,.65,.90,1.15,1.40,1.55,1.72,1.90)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("squat", "back", Equipment.BARBELL), MovementPattern.KNEE_DOMINANT, ratios(0.0,.50,.85,1.20,1.60,2.0,2.25,2.50,2.80)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("squat", "front", Equipment.BARBELL), MovementPattern.KNEE_DOMINANT, ratios(0.0,.40,.70,1.0,1.35,1.70,1.92,2.15,2.40)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("deadlift", "conventional", Equipment.BARBELL), MovementPattern.HIP_HINGE, ratios(0.0,.75,1.10,1.50,2.0,2.50,2.80,3.10,3.40)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("ohp", "standing", Equipment.BARBELL), MovementPattern.VERTICAL_PUSH, ratios(0.0,.30,.50,.70,.90,1.15,1.30,1.45,1.60)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("ohp", "seated", Equipment.DUMBBELL), MovementPattern.VERTICAL_PUSH, ratios(0.0,.22,.38,.55,.72,.90,1.02,1.14,1.28)),
    ExerciseStrengthStandard(ExerciseBenchmarkKey("pullup", "strict", Equipment.BODYWEIGHT), MovementPattern.VERTICAL_PULL, ratios(0.0,.85,1.0,1.12,1.30,1.50,1.62,1.75,1.90), LoadMode.BODYWEIGHT_PLUS_EXTERNAL),
)

private val aliases = mapOf(
    "barbell bench press" to ("bench" to "flat"), "bench press" to ("bench" to "flat"), "bench" to ("bench" to "flat"),
    "dumbbell bench press" to ("bench" to "flat"),
    "barbell bent over row" to ("row" to "bent_over"), "barbell row" to ("row" to "bent_over"),
    "barbell back squat" to ("squat" to "back"), "back squat" to ("squat" to "back"), "squat" to ("squat" to "back"),
    "front squat" to ("squat" to "front"),
    "deadlift" to ("deadlift" to "conventional"), "barbell deadlift" to ("deadlift" to "conventional"),
    "overhead press" to ("ohp" to "standing"), "barbell overhead press" to ("ohp" to "standing"), "ohp" to ("ohp" to "standing"),
    "dumbbell shoulder press" to ("ohp" to "seated"),
    "weighted pull-up" to ("pullup" to "strict"), "weighted pull up" to ("pullup" to "strict"),
    "pull-up" to ("pullup" to "strict"), "pull up" to ("pullup" to "strict"), "pullup" to ("pullup" to "strict"),
)

fun benchmarkForExercise(exerciseName: String, equipment: Equipment, variation: String = "standard"): ExerciseStrengthStandard? {
    val alias = aliases[exerciseName.trim().lowercase()] ?: return null
    val resolvedVariation = variation.trim().lowercase().takeUnless { it == "standard" } ?: alias.second
    return EXERCISE_STRENGTH_STANDARDS.firstOrNull {
        it.key.exercise == alias.first && it.key.variation == resolvedVariation && it.key.equipment == equipment
    }
}

private fun benchmarkFor(input: StrengthAssessmentInput) = benchmarkForExercise(input.exerciseId, input.equipment, input.variation)

fun movementForExerciseId(exerciseId: String): MovementPattern? {
    val alias = aliases[exerciseId.trim().lowercase()] ?: return null
    return EXERCISE_STRENGTH_STANDARDS.firstOrNull { it.key.exercise == alias.first && it.key.variation == alias.second }?.movement
}

private fun sexScale(sex: String?): Double = when (sex) { "male" -> 1.0; "female" -> .72; else -> .86 }

private fun totalExternalLoad(input: StrengthAssessmentInput): Double = when {
    input.equipment == Equipment.DUMBBELL && input.dumbbellWeightMode == DumbbellWeightMode.PER_HAND -> input.weightKg * 2.0
    input.equipment == Equipment.DUMBBELL && input.dumbbellWeightMode == null -> Double.NaN
    else -> input.weightKg
}

private fun relativeToScore(relative: Double, standard: ExerciseStrengthStandard, sex: String?, ageYears: Int?): Double {
    val ageScale = RankingV3Config.ageBands.firstOrNull { (ageYears ?: 30) in it.range }?.strengthScale ?: 1.0
    val scale = sexScale(sex) * ageScale
    val anchors = Rank.entries.map { rank ->
        Anchor(standard.maleRatios.getValue(rank) * scale, RANK_THRESHOLDS[rank.ordinal].min.toDouble())
    } + Anchor(standard.maleRatios.getValue(Rank.SSS) * scale * 1.08, 100.0)
    return clampScore(interpolate(anchors, relative))
}

fun scoreStrengthMovement(input: StrengthAssessmentInput, bodyweightKg: Double, sex: String?, ageYears: Int? = null): Double? {
    val standard = benchmarkFor(input) ?: return null
    if (bodyweightKg !in 30.0..300.0 || input.weightKg < 0.0 || input.reps !in 1..50) return null
    val external = totalExternalLoad(input)
    if (!external.isFinite() || external < 0.0) return null
    val rankedLoad = when (standard.loadMode) {
        LoadMode.EXTERNAL -> external
        LoadMode.BODYWEIGHT_PLUS_EXTERNAL -> bodyweightKg + external
    }
    if (rankedLoad <= 0.0) return null
    val estimated1rm = calculateEstimated1RM(rankedLoad, input.reps.coerceAtMost(12))
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
    val inferred = equipment ?: when {
        exerciseId.contains("pull", true) -> Equipment.BODYWEIGHT
        exerciseId.contains("dumbbell", true) -> Equipment.DUMBBELL
        else -> Equipment.BARBELL
    }
    val standard = benchmarkForExercise(exerciseId, inferred, variation) ?: return null
    if (estimated1RMkg <= 0.0 || bodyweightKg !in 30.0..300.0) return null
    return relativeToScore(estimated1RMkg / bodyweightKg, standard, sex, ageYears)
}

private fun combineMovementScores(scores: Collection<Double>): Double? = scores.takeIf { it.isNotEmpty() }?.let {
    clampScore(it.average() * RankingV3Config.STRENGTH_AVERAGE_WEIGHT + it.min() * RankingV3Config.STRENGTH_WEAKEST_WEIGHT)
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
    val scored = inputs.mapNotNull { input ->
        val standard = benchmarkFor(input) ?: return@mapNotNull null
        val score = scoreStrengthMovement(input, bodyweightKg, sex, ageYears) ?: return@mapNotNull null
        Triple(standard.movement, score, input)
    }
    if (inputs.any { benchmarkFor(it) == null }) reasons += "Non-standardized exercises use Personal Tier, not Global Strength Rank."
    if (inputs.any { it.reps > 12 }) reasons += "Sets above 12 repetitions are logged but cannot produce high global ranks."

    val movements = scored.groupBy { it.first }.mapValues { (_, values) -> values.maxOf { it.second } }
    val score = combineMovementScores(movements.values)
    if (score == null) return StrengthRankResult(null, null, emptyMap(), Rank.C, true, AssessmentConfidence.LOW, reasons + "No valid standardized strength benchmark is available.")

    val movementCount = movements.size
    val sessionCount = inputs.mapNotNull { it.sessionId }.distinct().size.coerceAtLeast(qualifyingSessionCount)
    val hasRecentDates = todayEpochDay != null && scored.isNotEmpty() && scored.all {
        it.third.performedAtEpochDay?.let { day -> todayEpochDay - day <= RankingV3Config.STRENGTH_VALID_DAYS } == true
    }
    val stale = todayEpochDay != null && !hasRecentDates
    val highRep = inputs.any { it.reps > 12 }

    val confidence = when {
        stale || movementCount < 3 || highRep -> AssessmentConfidence.LOW
        movementCount >= 5 && sessionCount >= 3 && hasRecentDates -> AssessmentConfidence.HIGH
        else -> AssessmentConfidence.MEDIUM
    }
    val cap = when {
        movementCount <= 1 -> Rank.C
        movementCount == 2 -> Rank.B
        stale || highRep || sessionCount < 2 -> Rank.B
        movementCount == 3 -> Rank.A
        movementCount == 4 && sessionCount < 3 -> Rank.S
        movementCount == 4 -> Rank.S_PLUS
        movementCount >= 5 && sessionCount < 3 -> Rank.S
        movementCount >= 5 && confidence != AssessmentConfidence.HIGH -> Rank.S_PLUS
        movementCount >= 5 && sessionCount < 5 -> Rank.SS
        else -> Rank.SSS
    }
    if (movementCount < 4) reasons += "S requires at least four validated movement patterns."
    if (movementCount < 5) reasons += "SS and SSS require at least five validated movement patterns."
    if (sessionCount < 3) reasons += "Elite Strength ranks require repeated validating sessions."
    if (stale) reasons += "Strength assessment update recommended."

    val confidenceCap = when (confidence) {
        AssessmentConfidence.LOW -> Rank.C
        AssessmentConfidence.MEDIUM -> Rank.A
        AssessmentConfidence.HIGH -> Rank.SSS
    }
    val effectiveCap = minOf(cap, confidenceCap)
    val rank = scoreToRank(score).let { if (it.ordinal > effectiveCap.ordinal) effectiveCap else it }
    return StrengthRankResult(
        score = score,
        rank = rank,
        movementScores = movements.mapKeys { it.key.name },
        rankCap = cap,
        provisional = movementCount < 3 || stale,
        confidence = confidence,
        reasons = reasons.distinct(),
        rp = if (rank == scoreToRank(score)) scoreToRp(score) else 99,
    )
}

fun computeStrengthScore(inputs: List<StrengthAssessmentInput>, bodyweightKg: Double, sex: String?): Double? =
    computeStrengthRank(inputs, bodyweightKg, sex).score

fun computeStrengthScoreFromEstimated1RMs(items: List<Pair<String, Double>>, bodyweightKg: Double, sex: String?): Double? =
    combineMovementScores(items.mapNotNull { scoreStrengthFromEstimated1RM(it.first, it.second, bodyweightKg, sex) })

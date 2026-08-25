package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.ranking.Anchor
import com.fitnessrpg.app.domain.ranking.interpolate

/**
 * Strength Score across major movements, normalized by bodyweight. Each movement
 * has its OWN standards (bench != squat), equipment is normalized to a barbell
 * equivalent, and imbalances are penalized (never `max(exerciseScores)`).
 */

/** Per-exercise relative-1RM (est1RM / bodyweight) thresholds for a trained MALE. */
data class ExerciseStrengthStandard(val movement: String, val thresholds: Map<Rank, Double>)

private val STRENGTH_STANDARDS: Map<String, ExerciseStrengthStandard> = mapOf(
    "bench" to ExerciseStrengthStandard(
        "bench",
        mapOf(Rank.E to 0.0, Rank.D to 0.45, Rank.C to 0.70, Rank.B to 1.00, Rank.A to 1.30, Rank.S to 1.60),
    ),
    "squat" to ExerciseStrengthStandard(
        "squat",
        mapOf(Rank.E to 0.0, Rank.D to 0.50, Rank.C to 0.85, Rank.B to 1.20, Rank.A to 1.60, Rank.S to 2.00),
    ),
    "deadlift" to ExerciseStrengthStandard(
        "deadlift",
        mapOf(Rank.E to 0.0, Rank.D to 0.75, Rank.C to 1.10, Rank.B to 1.50, Rank.A to 2.00, Rank.S to 2.50),
    ),
    "ohp" to ExerciseStrengthStandard(
        "ohp",
        mapOf(Rank.E to 0.0, Rank.D to 0.30, Rank.C to 0.50, Rank.B to 0.70, Rank.A to 0.90, Rank.S to 1.15),
    ),
    "row" to ExerciseStrengthStandard(
        "row",
        mapOf(Rank.E to 0.0, Rank.D to 0.45, Rank.C to 0.70, Rank.B to 0.95, Rank.A to 1.20, Rank.S to 1.50),
    ),
)

/** Score bands aligned to rank lower bounds. */
private val BAND_SCORES = listOf(0.0, 20.0, 35.0, 50.0, 65.0, 80.0)

/** Sex scaling of the male standards (lower threshold => same lift scores higher). */
private fun sexScale(sex: String?): Double = when (sex) {
    "male" -> 1.0
    "female" -> 0.72
    else -> 0.86
}

/** Barbell-equivalent factor for equipment (provisional, tunable). */
private fun equipmentFactor(equipment: Equipment): Double = when (equipment) {
    Equipment.BARBELL -> 1.0
    Equipment.SMITH_MACHINE -> 0.95
    Equipment.DUMBBELL -> 0.90
    Equipment.MACHINE -> 0.85
    Equipment.BODYWEIGHT -> 1.0
    Equipment.OTHER -> 0.85
}

/** Map a catalog exercise name (or a bare movement key) to a standard movement. */
private val NAME_TO_MOVEMENT: Map<String, String> = mapOf(
    "bench" to "bench", "squat" to "squat", "deadlift" to "deadlift", "ohp" to "ohp", "row" to "row",
    "Barbell Bench Press" to "bench", "Dumbbell Bench Press" to "bench", "Incline Dumbbell Press" to "bench",
    "Barbell Back Squat" to "squat", "Front Squat" to "squat", "Smith Machine Squat" to "squat",
    "Goblet Squat" to "squat", "Leg Press" to "squat",
    "Deadlift" to "deadlift", "Romanian Deadlift" to "deadlift",
    "Overhead Press" to "ohp", "Barbell Bent-Over Row" to "row",
)

fun movementForExerciseId(exerciseId: String): String? = NAME_TO_MOVEMENT[exerciseId]

/** Total lifted weight for the input, resolving dumbbell per-hand vs combined. */
private fun totalLoadKg(input: StrengthAssessmentInput): Double = when {
    input.equipment == Equipment.DUMBBELL &&
        (input.dumbbellWeightMode ?: DumbbellWeightMode.PER_HAND) == DumbbellWeightMode.PER_HAND ->
        input.weightKg * 2.0
    else -> input.weightKg
}

/**
 * Score a single movement 0..100 from a set, or null if the movement has no
 * standard. Reps are clamped to 12 for ranking so high-rep sets can't inflate 1RM.
 */
/** Map a bodyweight-relative 1RM to a 0..100 score for a movement + sex. */
private fun relativeToScore(relative: Double, movement: String, sex: String?): Double? {
    val standard = STRENGTH_STANDARDS[movement] ?: return null
    val scale = sexScale(sex)
    val ranks = listOf(Rank.E, Rank.D, Rank.C, Rank.B, Rank.A, Rank.S)
    val anchors = buildList {
        add(Anchor(0.0, 0.0))
        ranks.forEachIndexed { i, r ->
            if (i > 0) add(Anchor(standard.thresholds.getValue(r) * scale, BAND_SCORES[i]))
        }
        val top = standard.thresholds.getValue(Rank.S) * scale
        add(Anchor(top * 1.3, 100.0))
    }
    return clampScore(interpolate(anchors, relative))
}

fun scoreStrengthMovement(input: StrengthAssessmentInput, bodyweightKg: Double, sex: String?): Double? {
    val movement = movementForExerciseId(input.exerciseId) ?: return null
    if (bodyweightKg <= 0.0 || input.weightKg <= 0.0 || input.reps < 1) return null

    val repsForRanking = input.reps.coerceIn(1, 12)
    val est1rm = calculateEstimated1RM(totalLoadKg(input), repsForRanking) * equipmentFactor(input.equipment)
    return relativeToScore(est1rm / bodyweightKg, movement, sex)
}

/**
 * Score a movement from an already-estimated 1RM (used by the post-workout path,
 * where best 1RMs are stored per exercise). No equipment factor — the stored 1RM
 * already reflects how the set was logged.
 */
fun scoreStrengthFromEstimated1RM(exerciseId: String, estimated1RMkg: Double, bodyweightKg: Double, sex: String?): Double? {
    val movement = movementForExerciseId(exerciseId) ?: return null
    if (bodyweightKg <= 0.0 || estimated1RMkg <= 0.0) return null
    return relativeToScore(estimated1RMkg / bodyweightKg, movement, sex)
}

/**
 * Combined Strength Score across the assessed movements. Uses an average with a
 * strong weakest-movement penalty so a strong bench can't hide a weak squat.
 * Null when no assessable movement is present.
 */
fun computeStrengthScore(
    inputs: List<StrengthAssessmentInput>,
    bodyweightKg: Double,
    sex: String?,
): Double? = combineMovementScores(inputs.mapNotNull { scoreStrengthMovement(it, bodyweightKg, sex) })

/** Combined Strength Score from stored best 1RMs (exerciseId -> estimated1RMkg). */
fun computeStrengthScoreFromEstimated1RMs(
    items: List<Pair<String, Double>>,
    bodyweightKg: Double,
    sex: String?,
): Double? = combineMovementScores(
    items.mapNotNull { scoreStrengthFromEstimated1RM(it.first, it.second, bodyweightKg, sex) },
)

/** Average with a strong weakest-movement penalty (never `max`). */
private fun combineMovementScores(scores: List<Double>): Double? {
    if (scores.isEmpty()) return null
    return clampScore(scores.average() * 0.6 + scores.min() * 0.4)
}

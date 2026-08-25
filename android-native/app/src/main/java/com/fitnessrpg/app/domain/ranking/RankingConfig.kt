package com.fitnessrpg.app.domain.ranking

/**
 * Ranking configuration — ALL tunable constants live here, isolated from logic
 * and UI (PLAN.txt §6). Values marked PROVISIONAL are seed estimates. Bump
 * [RANKING_VERSION] when the math or these constants change.
 */
const val RANKING_VERSION = 2

/** Gate Score component weights (PLAN.txt §6.5). Must sum to 1. */
object GateWeights {
    const val PERFORMANCE = 0.30
    const val COMPLETION = 0.35
    const val PROGRESS = 0.25
    const val PR = 0.1
    const val QUALITY = 0.0
}

/** Hunter Rank attribute weights (PLAN.txt §6.7). Must sum to 1. */
object HunterWeights {
    const val STRENGTH = 0.4
    const val PHYSIQUE = 0.35
    const val ENDURANCE = 0.25
    const val DISCIPLINE = 0.0
}

/** Neutral score used where a factor has no data yet (don't punish new users). */
const val NEUTRAL_SCORE = 60.0

/** Plausibility bounds for anti-inflation validation (PLAN.txt §6.6). */
object ValidationLimits {
    const val MIN_WEIGHT_KG = 0.0
    const val MAX_WEIGHT_KG = 600.0
    const val MIN_REPS = 1
    const val MAX_REPS = 100
    const val MIN_RPE = 0.0
    const val MAX_RPE = 10.0

    /** A qualifying performance needs at least this many valid working sets. */
    const val MIN_QUALIFYING_SETS = 2

    /** Reaching S rank must be demonstrated across at least this many sessions. */
    const val MIN_SESSIONS_FOR_S = 2

    /** A single rank update may not jump more than this many bands. */
    const val MAX_RANK_JUMP = 2
}

/**
 * Score anchors aligned to the rank-band lower bounds (PLAN.txt §6.1) plus the
 * top of the scale.
 */
val SCORE_ANCHORS = listOf(0.0, 20.0, 35.0, 50.0, 65.0, 80.0, 100.0)

enum class Movement { BENCH, SQUAT, DEADLIFT, OHP, ROW }

/**
 * PROVISIONAL strength standards: estimated-1RM as a multiple of bodyweight for
 * a trained MALE lifter, at each [SCORE_ANCHORS] value.
 */
private val MALE_RATIO_ANCHORS: Map<Movement, List<Double>> = mapOf(
    Movement.BENCH to listOf(0.2, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0),
    Movement.SQUAT to listOf(0.4, 0.75, 1.0, 1.25, 1.5, 1.75, 2.5),
    Movement.DEADLIFT to listOf(0.5, 1.0, 1.25, 1.5, 1.75, 2.0, 3.0),
    Movement.OHP to listOf(0.15, 0.35, 0.5, 0.6, 0.75, 0.9, 1.3),
    Movement.ROW to listOf(0.3, 0.55, 0.75, 0.95, 1.15, 1.35, 1.8),
)

/** PROVISIONAL sex scaling of the male ratio standards. */
private val SEX_SCALE = mapOf("male" to 1.0, "female" to 0.72, "neutral" to 0.86)

/** Map an exercise name to a strength-standard movement, or null if unranked. */
private val EXERCISE_MOVEMENT: Map<String, Movement> = mapOf(
    "Barbell Bench Press" to Movement.BENCH,
    "Barbell Back Squat" to Movement.SQUAT,
    "Front Squat" to Movement.SQUAT,
    "Deadlift" to Movement.DEADLIFT,
    "Overhead Press" to Movement.OHP,
    "Barbell Bent-Over Row" to Movement.ROW,
)

fun movementForExercise(name: String): Movement? = EXERCISE_MOVEMENT[name]

private fun sexKey(sex: String?): String = when (sex) {
    "male" -> "male"
    "female" -> "female"
    else -> "neutral"
}

/** Ratio->score anchors for a movement + sex (scaled male standards). */
fun strengthAnchors(movement: Movement, sex: String?): List<Anchor> {
    val scale = SEX_SCALE.getValue(sexKey(sex))
    val ratios = MALE_RATIO_ANCHORS.getValue(movement)
    return ratios.mapIndexed { i, r -> Anchor(r * scale, SCORE_ANCHORS[i]) }
}

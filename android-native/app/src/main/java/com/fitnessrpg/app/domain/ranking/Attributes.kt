package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRank

/**
 * Hunter attributes -> Hunter Rank (PLAN.txt §6.7-6.9). Strength/Physique/
 * Endurance/Discipline each 0..100, combined by [HunterWeights]. All models here
 * are PROVISIONAL and use healthy, non-punitive ranges — no medical claims.
 */
data class HunterAttributes(
    val strength: Double?,
    val physique: Double?,
    val endurance: Double?,
    val discipline: Double?,
)

/** Strength attribute: average of the user's ranked exercise scores. */
fun strengthScore(exerciseScores: List<Double>): Double? {
    if (exerciseScores.isEmpty()) return null
    val sum = exerciseScores.sumOf { clampScore(it) }
    return clampScore(sum / exerciseScores.size)
}

// PROVISIONAL healthy body-fat curves (bodyFat% -> score). Peak in a healthy
// band; not rewarding for ever-lower body fat.
private val BODYFAT_MALE: List<Anchor> = listOf(
    Anchor(4.0, 60.0), Anchor(8.0, 85.0), Anchor(12.0, 100.0), Anchor(18.0, 90.0),
    Anchor(25.0, 65.0), Anchor(32.0, 40.0), Anchor(40.0, 20.0),
)
private val BODYFAT_FEMALE: List<Anchor> = listOf(
    Anchor(12.0, 60.0), Anchor(16.0, 85.0), Anchor(22.0, 100.0), Anchor(28.0, 90.0),
    Anchor(34.0, 65.0), Anchor(40.0, 40.0), Anchor(48.0, 20.0),
)

// PROVISIONAL muscle development: skeletal-muscle mass as % of bodyweight.
private val MUSCLE_DEV: List<Anchor> = listOf(
    Anchor(30.0, 30.0), Anchor(38.0, 60.0), Anchor(44.0, 90.0), Anchor(50.0, 100.0),
)

data class PhysiqueInput(
    val bodyFatPercent: Double?,
    val skeletalMuscleMassKg: Double?,
    val weightKg: Double?,
    val sex: String?,
)

/** Physique attribute from body composition. Null when there is no assessment. */
fun physiqueScore(a: PhysiqueInput?): Double? {
    if (a == null) return null
    val parts = mutableListOf<WeightedComponent>()

    if (a.bodyFatPercent != null) {
        val curve = if (a.sex == "female") BODYFAT_FEMALE else BODYFAT_MALE
        parts.add(WeightedComponent(clampScore(interpolate(curve, a.bodyFatPercent)), 0.5))
    }
    val smm = a.skeletalMuscleMassKg
    val weight = a.weightKg
    if (smm != null && weight != null && weight > 0.0) {
        val smmPct = (smm / weight) * 100.0
        val scale = if (a.sex == "female") 1.1 else 1.0 // provisional: women carry less SMM%
        parts.add(WeightedComponent(clampScore(interpolate(MUSCLE_DEV, smmPct * scale)), 0.5))
    }
    if (parts.isEmpty()) return null
    return weightedRenormalized(parts)
}

// PROVISIONAL endurance from weekly training minutes (guideline ~150 min).
private val ENDURANCE_MINUTES: List<Anchor> = listOf(
    Anchor(0.0, 0.0), Anchor(75.0, 45.0), Anchor(150.0, 70.0), Anchor(300.0, 100.0),
)

/** Endurance attribute from recent training volume. Null when no data. */
fun enduranceScore(weeklyTrainingMinutes: Double?): Double? {
    if (weeklyTrainingMinutes == null) return null
    return clampScore(interpolate(ENDURANCE_MINUTES, weeklyTrainingMinutes))
}

/** Combined Hunter score, renormalized over available attributes. */
fun hunterScore(attrs: HunterAttributes): Double = weightedRenormalized(
    listOf(
        WeightedComponent(attrs.strength, HunterWeights.STRENGTH),
        WeightedComponent(attrs.physique, HunterWeights.PHYSIQUE),
        WeightedComponent(attrs.endurance, HunterWeights.ENDURANCE),
        WeightedComponent(attrs.discipline, HunterWeights.DISCIPLINE),
    ),
)

fun hunterRank(score: Double): Rank = scoreToRank(score)

enum class AttributeName { STRENGTH, PHYSIQUE, ENDURANCE, DISCIPLINE }

/** The lowest available attribute — the one most limiting the next rank. */
fun limitingAttribute(attrs: HunterAttributes): AttributeName? {
    var name: AttributeName? = null
    var lowest = Double.POSITIVE_INFINITY
    // Iteration order matches the attribute declaration order; strict `<` keeps
    // the earlier attribute on ties.
    val pairs = listOf(
        AttributeName.STRENGTH to attrs.strength,
        AttributeName.PHYSIQUE to attrs.physique,
        AttributeName.ENDURANCE to attrs.endurance,
        AttributeName.DISCIPLINE to attrs.discipline,
    )
    for ((key, value) in pairs) {
        if (value != null && value < lowest) {
            lowest = value
            name = key
        }
    }
    return name
}

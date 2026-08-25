package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.ranking.Anchor
import com.fitnessrpg.app.domain.ranking.interpolate

/**
 * Physique Score from balanced body composition. Explicitly does NOT reward
 * ever-lower body fat, and does NOT treat total muscle mass as skeletal muscle
 * mass. BMI is never used as the ranking metric. Returns null when no usable
 * body-composition data is present.
 *
 * Two components (each 0..100, averaged when both present):
 *  1. Body-fat position within a healthy/athletic band (peaks in-band, tapers out).
 *  2. Muscular development, from the BEST available signal in priority order:
 *     skeletal muscle mass % -> FFMI (lean mass / height²) -> total muscle mass %.
 */

// PROVISIONAL body-fat curves (bodyFat% -> score). Male peak ~11-15%, female ~19-23%.
private val BODYFAT_MALE: List<Anchor> = listOf(
    Anchor(4.0, 55.0), Anchor(8.0, 82.0), Anchor(12.0, 100.0), Anchor(15.0, 96.0),
    Anchor(18.0, 82.0), Anchor(22.0, 64.0), Anchor(28.0, 46.0), Anchor(35.0, 28.0), Anchor(45.0, 12.0),
)
private val BODYFAT_FEMALE: List<Anchor> = listOf(
    Anchor(10.0, 55.0), Anchor(15.0, 82.0), Anchor(20.0, 100.0), Anchor(23.0, 96.0),
    Anchor(26.0, 82.0), Anchor(30.0, 64.0), Anchor(36.0, 46.0), Anchor(42.0, 28.0), Anchor(52.0, 12.0),
)

// PROVISIONAL skeletal-muscle-mass %-of-bodyweight -> score (TRUE SMM only).
private val SMM_PCT_MALE: List<Anchor> = listOf(Anchor(30.0, 30.0), Anchor(38.0, 60.0), Anchor(44.0, 90.0), Anchor(50.0, 100.0))
private val SMM_PCT_FEMALE: List<Anchor> = listOf(Anchor(26.0, 30.0), Anchor(33.0, 60.0), Anchor(38.0, 90.0), Anchor(44.0, 100.0))

// PROVISIONAL Fat-Free Mass Index (lean mass / height²) -> score.
private val FFMI_MALE: List<Anchor> = listOf(Anchor(16.0, 25.0), Anchor(18.0, 45.0), Anchor(20.0, 62.0), Anchor(22.0, 80.0), Anchor(24.0, 92.0), Anchor(26.0, 100.0))
private val FFMI_FEMALE: List<Anchor> = listOf(Anchor(13.0, 25.0), Anchor(15.0, 45.0), Anchor(17.0, 62.0), Anchor(19.0, 80.0), Anchor(21.0, 92.0), Anchor(23.0, 100.0))

// PROVISIONAL total muscle-mass %-of-bodyweight -> score (conservative; NOT SMM anchors).
private val MUSCLE_PCT_MALE: List<Anchor> = listOf(Anchor(60.0, 30.0), Anchor(70.0, 60.0), Anchor(78.0, 88.0), Anchor(84.0, 100.0))
private val MUSCLE_PCT_FEMALE: List<Anchor> = listOf(Anchor(54.0, 30.0), Anchor(64.0, 60.0), Anchor(72.0, 88.0), Anchor(80.0, 100.0))

private fun isFemale(sex: String?) = sex == "female"

private fun bodyFatScore(bodyFat: Double, female: Boolean): Double =
    clampScore(interpolate(if (female) BODYFAT_FEMALE else BODYFAT_MALE, bodyFat))

/**
 * Muscular development from the best available signal. Priority guarantees that a
 * total-muscle-mass number is NEVER interpreted with skeletal-muscle-mass anchors.
 */
private fun muscleDevelopmentScore(body: BodyCompositionData, female: Boolean): Double? {
    val weight = body.weightKg
    if (weight <= 0.0) return null

    body.skeletalMuscleMassKg?.let { smm ->
        val pct = smm / weight * 100.0
        return clampScore(interpolate(if (female) SMM_PCT_FEMALE else SMM_PCT_MALE, pct))
    }

    val lean = body.leanBodyMassKg ?: body.bodyFatPercent?.let { weight * (1.0 - it / 100.0) }
    val heightM = body.heightCm / 100.0
    if (lean != null && heightM > 0.0) {
        val ffmi = lean / (heightM * heightM)
        return clampScore(interpolate(if (female) FFMI_FEMALE else FFMI_MALE, ffmi))
    }

    body.muscleMassKg?.let { mm ->
        val pct = mm / weight * 100.0
        return clampScore(interpolate(if (female) MUSCLE_PCT_FEMALE else MUSCLE_PCT_MALE, pct))
    }

    return null
}

fun computePhysiqueScore(body: BodyCompositionData): Double? {
    val female = isFemale(body.sex)
    val parts = mutableListOf<Double>()
    body.bodyFatPercent?.let { parts.add(bodyFatScore(it, female)) }
    muscleDevelopmentScore(body, female)?.let { parts.add(it) }
    if (parts.isEmpty()) return null
    return clampScore(parts.average())
}

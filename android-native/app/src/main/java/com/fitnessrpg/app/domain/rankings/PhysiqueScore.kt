package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.ranking.Anchor
import com.fitnessrpg.app.domain.ranking.interpolate
import kotlin.math.abs
import kotlin.math.pow

private val balanceCurve = listOf(
    Anchor(0.0, 100.0), Anchor(2.0, 95.0), Anchor(5.0, 75.0),
    Anchor(10.0, 45.0), Anchor(15.0, 20.0), Anchor(25.0, 0.0),
)

fun calculateFfmi(weightKg: Double, heightCm: Double, bodyFatPercent: Double): Double? {
    if (weightKg !in 30.0..300.0 || heightCm !in 100.0..250.0 || bodyFatPercent !in 3.0..60.0) return null
    val heightM = heightCm / 100.0
    return weightKg * (1.0 - bodyFatPercent / 100.0) / heightM.pow(2)
}

private fun pairAsymmetry(left: Double?, right: Double?): Double? {
    if (left == null || right == null || left <= 0.0 || right <= 0.0) return null
    return abs(left - right) / ((left + right) / 2.0) * 100.0
}

fun calculateMuscleBalanceScore(data: SegmentalLeanMassData?): Double? {
    if (data == null) return null
    val asymmetries = listOfNotNull(
        pairAsymmetry(data.leftArmKg, data.rightArmKg),
        pairAsymmetry(data.leftLegKg, data.rightLegKg),
    )
    if (asymmetries.isEmpty()) return null
    return clampScore(interpolate(balanceCurve, asymmetries.average()))
}

fun computePhysiqueRank(body: BodyCompositionData, todayEpochDay: Long? = null): PhysiqueRankResult {
    val reasons = mutableListOf<String>()
    if (body.weightKg !in 30.0..300.0 || body.heightCm !in 100.0..250.0) {
        return PhysiqueRankResult(
            score = null, rank = null, bodyCompositionScore = null, muscularityScore = null,
            waistScore = null, balanceScore = null, rankCap = Rank.C, provisional = true,
            confidence = AssessmentConfidence.LOW, reasons = listOf("Valid height and weight are required."),
        )
    }

    val female = body.sex == "female"
    val sexKnown = female || body.sex == "male"
    if (!sexKnown) reasons += "Sex-specific body-composition benchmarks are unavailable."
    val ageBand = RankingV2Config.ageBands.firstOrNull { (body.ageYears ?: 30) in it.range }
        ?: RankingV2Config.ageBands.first()

    val validBodyFat = body.bodyFatPercent?.takeIf { it in 3.0..60.0 }
    if (body.bodyFatPercent != null && validBodyFat == null) reasons += "Body-fat percentage is outside the plausible range."
    val composition = if (sexKnown) validBodyFat?.let {
        clampScore(interpolate(if (female) RankingV2Config.femaleBodyFat else RankingV2Config.maleBodyFat, it - ageBand.bodyFatAllowance))
    } else null

    val explicitLean = body.leanBodyMassKg?.takeIf { it > 0.0 && it <= body.weightKg }
    val derivedLean = validBodyFat?.let { body.weightKg * (1.0 - it / 100.0) }
    val ffmi = (explicitLean ?: derivedLean)?.let { it / (body.heightCm / 100.0).pow(2) }
    val muscularity = if (!sexKnown) null else ffmi?.let {
        clampScore(interpolate(if (female) RankingV2Config.femaleFfmi else RankingV2Config.maleFfmi, it))
    } ?: body.skeletalMuscleMassKg?.takeIf { it > 0.0 && it <= body.weightKg }?.let {
        clampScore(interpolate(if (female) RankingV2Config.femaleSmmPercent else RankingV2Config.maleSmmPercent, it / body.weightKg * 100.0))
    }

    val waist = body.waistCm?.takeIf { it in 40.0..200.0 }?.let {
        clampScore(interpolate(RankingV2Config.waistToHeight, it / body.heightCm))
    }
    if (body.waistCm != null && waist == null) reasons += "Waist circumference is outside the plausible range."
    val balance = calculateMuscleBalanceScore(body.segmentalLeanMass)

    val components = listOfNotNull(
        composition?.let { it to RankingV2Config.PHYSIQUE_COMPOSITION_WEIGHT },
        muscularity?.let { it to RankingV2Config.PHYSIQUE_MUSCULARITY_WEIGHT },
        waist?.let { it to RankingV2Config.PHYSIQUE_WAIST_WEIGHT },
        balance?.let { it to RankingV2Config.PHYSIQUE_BALANCE_WEIGHT },
    )
    val score = components.takeIf { it.isNotEmpty() }?.let { values ->
        clampScore(values.sumOf { it.first * it.second } / values.sumOf { it.second })
    }

    var cap = Rank.S
    if (composition == null) { cap = minOf(cap, Rank.C); reasons += "Reliable body-fat data is missing." }
    if (waist == null) { cap = minOf(cap, Rank.B); reasons += "Waist measurement is missing." }
    if (muscularity == null) { cap = minOf(cap, Rank.C); reasons += "Muscularity could not be calculated from FFMI or explicit SMM." }
    if (balance == null) { cap = minOf(cap, Rank.A); reasons += "Segmental muscle-balance data is unavailable; S Physique is locked." }
    if (body.muscleMassKg != null) reasons += "Generic muscle mass is retained as a separate metric and does not increase Physique Rank."

    val stale = todayEpochDay != null && (body.assessedAtEpochDay == null || todayEpochDay - body.assessedAtEpochDay > RankingV2Config.BODY_ASSESSMENT_VALID_DAYS)
    if (stale) { cap = minOf(cap, Rank.A); reasons += "Body assessment update recommended." }
    val missingCore = composition == null || muscularity == null || waist == null
    val confidence = when {
        stale || missingCore -> AssessmentConfidence.LOW
        balance != null && body.assessedAtEpochDay != null -> AssessmentConfidence.HIGH
        else -> AssessmentConfidence.MEDIUM
    }
    val provisional = score == null || missingCore || stale

    var rank: Rank? = if (score == null) null else Rank.E
    if (score != null) for (candidate in Rank.entries) {
        val req = RankingV2Config.physiqueRequirements.getValue(candidate)
        val passesFloors = score >= req.overall &&
            (composition == null || composition >= req.composition) &&
            (muscularity == null || muscularity >= req.muscularity) &&
            (waist == null || waist >= req.waist) &&
            (req.balance == null || (balance ?: -1.0) >= req.balance)
        if (passesFloors && candidate.ordinal <= cap.ordinal) rank = candidate
    }

    return PhysiqueRankResult(
        score = score, rank = rank, bodyCompositionScore = composition, muscularityScore = muscularity,
        waistScore = waist, balanceScore = balance, rankCap = cap, provisional = provisional,
        confidence = confidence, stale = stale, reasons = reasons.distinct(),
    )
}

fun computePhysiqueScore(body: BodyCompositionData): Double? = computePhysiqueRank(body).score

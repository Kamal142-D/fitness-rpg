package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.ranking.Anchor
import com.fitnessrpg.app.domain.ranking.interpolate

private val maleBodyFat = listOf(Anchor(4.0, 40.0), Anchor(8.0, 75.0), Anchor(12.0, 100.0), Anchor(16.0, 92.0), Anchor(20.0, 72.0), Anchor(25.0, 48.0), Anchor(35.0, 18.0))
private val femaleBodyFat = listOf(Anchor(10.0, 40.0), Anchor(16.0, 75.0), Anchor(21.0, 100.0), Anchor(25.0, 92.0), Anchor(30.0, 70.0), Anchor(38.0, 40.0), Anchor(48.0, 15.0))
private val maleFfmi = listOf(Anchor(15.0, 15.0), Anchor(18.0, 42.0), Anchor(20.0, 60.0), Anchor(22.0, 75.0), Anchor(24.0, 88.0), Anchor(26.0, 96.0))
private val femaleFfmi = listOf(Anchor(12.0, 15.0), Anchor(15.0, 42.0), Anchor(17.0, 60.0), Anchor(19.0, 75.0), Anchor(21.0, 88.0), Anchor(23.0, 96.0))
private val maleSmmPct = listOf(Anchor(25.0, 15.0), Anchor(32.0, 40.0), Anchor(38.0, 62.0), Anchor(44.0, 82.0), Anchor(50.0, 96.0))
private val femaleSmmPct = listOf(Anchor(20.0, 15.0), Anchor(27.0, 40.0), Anchor(33.0, 62.0), Anchor(38.0, 82.0), Anchor(44.0, 96.0))
private val bodyFatAgeAllowance = listOf(0..39 to 0.0, 40..59 to 2.0, 60..120 to 4.0)
private val waistCurve = listOf(Anchor(0.38, 90.0), Anchor(0.42, 100.0), Anchor(0.47, 88.0), Anchor(0.50, 72.0), Anchor(0.55, 45.0), Anchor(0.65, 12.0))
private data class Req(val overall: Int, val composition: Int, val muscularity: Int, val waist: Int)
private val reqs = mapOf(Rank.E to Req(0,0,0,0), Rank.D to Req(20,15,10,10), Rank.C to Req(35,30,25,25), Rank.B to Req(50,45,45,40), Rank.A to Req(70,65,65,60), Rank.S to Req(85,80,80,75))

fun calculateFfmi(weightKg: Double, heightCm: Double, bodyFatPercent: Double): Double? {
    if (weightKg <= 0 || heightCm !in 120.0..230.0 || bodyFatPercent !in 3.0..60.0) return null
    val h = heightCm / 100.0
    return weightKg * (1.0 - bodyFatPercent / 100.0) / (h * h)
}

fun computePhysiqueRank(body: BodyCompositionData, todayEpochDay: Long? = null): PhysiqueRankResult {
    val reasons = mutableListOf<String>()
    val female = body.sex == "female"
    val ageAllowance = bodyFatAgeAllowance.firstOrNull { (range, _) -> (body.ageYears ?: 30) in range }?.second ?: 0.0
    val composition = body.bodyFatPercent?.takeIf { it in 3.0..60.0 }?.let { clampScore(interpolate(if (female) femaleBodyFat else maleBodyFat, it - ageAllowance)) }
    val ffmi = body.bodyFatPercent?.let { calculateFfmi(body.weightKg, body.heightCm, it) }
        ?: body.leanBodyMassKg?.takeIf { it > 0 && body.heightCm in 120.0..230.0 }?.let { it / Math.pow(body.heightCm / 100.0, 2.0) }
    val muscularity = ffmi?.let { clampScore(interpolate(if (female) femaleFfmi else maleFfmi, it)) }
        ?: body.skeletalMuscleMassKg?.takeIf { it > 0 && it <= body.weightKg }?.let { clampScore(interpolate(if (female) femaleSmmPct else maleSmmPct, it / body.weightKg * 100.0)) }
    val waist = body.waistCm?.takeIf { it in 40.0..200.0 && body.heightCm in 120.0..230.0 }?.let { clampScore(interpolate(waistCurve, it / body.heightCm)) }
    val weighted = listOfNotNull(composition?.let { it to .30 }, muscularity?.let { it to .35 }, waist?.let { it to .25 })
    val score = weighted.takeIf { it.isNotEmpty() }?.let { p -> clampScore(p.sumOf { it.first * it.second } / p.sumOf { it.second }) }
    var cap = Rank.S
    if (composition == null) { cap = Rank.C; reasons += "Reliable body-fat data is missing." }
    if (waist == null && cap.ordinal > Rank.B.ordinal) { cap = Rank.B; reasons += "Waist measurement is missing." }
    if (muscularity == null) { cap = minOf(cap, Rank.C); reasons += "Muscularity could not be calculated." }
    val stale = todayEpochDay != null && body.assessedAtEpochDay?.let { todayEpochDay - it > 90 } != false
    if (stale) { cap = minOf(cap, Rank.A); reasons += "Body assessment update recommended." }
    if (body.muscleMassKg != null) reasons += "Generic muscle mass does not increase Physique Rank."
    var rank: Rank? = if (score == null) null else Rank.E
    if (score != null) for (candidate in Rank.entries) {
        val r = reqs.getValue(candidate)
        val waistPass = waist?.let { it >= r.waist } ?: (candidate.ordinal <= Rank.B.ordinal)
        if (score >= r.overall && (composition ?: -1.0) >= r.composition && (muscularity ?: -1.0) >= r.muscularity && waistPass && candidate.ordinal <= cap.ordinal) rank = candidate
    }
    return PhysiqueRankResult(score, rank, composition, muscularity, waist, null, cap, reasons.isNotEmpty(), reasons)
}

fun computePhysiqueScore(body: BodyCompositionData): Double? = computePhysiqueRank(body).score

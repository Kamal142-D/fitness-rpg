package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore

/**
 * Compute the Hunter Rank from the three physical pillars. Discipline is NOT an
 * input here by design.
 *
 * Score:  base = physique·0.40 + strength·0.40 + conditioning·0.20 (renormalized
 * over the pillars that are actually assessed); then
 *   hunterScore = base·0.70 + weakestAssessedPillar·0.30.
 *
 * Rank: the highest rank whose overall-score AND every pillar minimum are met.
 * An unassessed pillar can't satisfy a minimum above the provisional cap, and any
 * missing pillar makes the result provisional (capped at [PROVISIONAL_MAX_RANK]).
 */
fun computeHunterRank(
    physiqueScore: Double?,
    strengthScore: Double?,
    conditioningScore: Double?,
    evidenceConfidence: AssessmentConfidence? = null,
): HunterRankResult {
    val missing = listOf(physiqueScore, strengthScore, conditioningScore).count { it == null }
    val confidence = evidenceConfidence ?: if (missing > 0) AssessmentConfidence.LOW else AssessmentConfidence.MEDIUM
    val provisional = confidence != AssessmentConfidence.HIGH

    // Weighted base over the assessed pillars (renormalized), plus weakest penalty.
    val weighted = buildList {
        physiqueScore?.let { add(it to 0.35) }
        strengthScore?.let { add(it to 0.40) }
        conditioningScore?.let { add(it to 0.25) }
    }
    val base = if (weighted.isNotEmpty()) {
        weighted.sumOf { it.first * it.second } / weighted.sumOf { it.second }
    } else {
        0.0
    }
    val known = listOfNotNull(physiqueScore, strengthScore, conditioningScore)
    val weakest = known.minOrNull() ?: 0.0
    val hunterScore = clampScore(base * 0.80 + weakest * 0.20)

    val provisionalCapIndex = PROVISIONAL_MAX_RANK.ordinal

    fun pillarPasses(score: Double?, min: Int, rankIndex: Int): Boolean = when {
        min == 0 -> true
        score == null -> rankIndex <= provisionalCapIndex
        else -> score >= min
    }

    fun meets(rank: Rank): Boolean {
        val req = HUNTER_RANK_REQUIREMENTS.getValue(rank)
        val idx = rank.ordinal
        return hunterScore >= req.minHunterScore &&
            pillarPasses(physiqueScore, req.minPhysique, idx) &&
            pillarPasses(strengthScore, req.minStrength, idx) &&
            pillarPasses(conditioningScore, req.minConditioning, idx)
    }

    var chosen = Rank.E
    for (rank in Rank.entries) {
        if (meets(rank)) chosen = rank
    }
    val confidenceCap = when (confidence) {
        AssessmentConfidence.LOW -> Rank.C
        AssessmentConfidence.MEDIUM -> Rank.A
        AssessmentConfidence.HIGH -> Rank.S
    }
    val cap = if (missing > 0) minOf(PROVISIONAL_MAX_RANK, confidenceCap) else confidenceCap
    if (chosen.ordinal > cap.ordinal) chosen = cap

    val next = Rank.entries.getOrNull(chosen.ordinal + 1)
    val limiting = next?.let { limitingAttributeFor(it, physiqueScore, strengthScore, conditioningScore) }
    val nextInfo = next?.let {
        val req = HUNTER_RANK_REQUIREMENTS.getValue(it)
        NextRankInfo(it, req.minPhysique, req.minStrength, req.minConditioning, req.minHunterScore)
    }

    return HunterRankResult(
        rank = chosen,
        hunterScore = hunterScore,
        physiqueScore = physiqueScore,
        strengthScore = strengthScore,
        conditioningScore = conditioningScore,
        limitingAttribute = limiting,
        provisional = provisional,
        confidence = confidence,
        nextRank = nextInfo,
        rankCap = cap,
        reasons = buildList {
            if (physiqueScore == null) add("Physique has not been fully assessed.")
            if (strengthScore == null) add("Strength assessment is incomplete.")
            if (conditioningScore == null) add("Conditioning has not been assessed.")
            if (confidence != AssessmentConfidence.HIGH) add("More recent validated evidence is needed for higher ranks.")
        },
    )
}

/** The pillar with the largest shortfall against the next rank's requirements. */
private fun limitingAttributeFor(
    next: Rank,
    physique: Double?,
    strength: Double?,
    conditioning: Double?,
): PhysicalAttribute? {
    val req = HUNTER_RANK_REQUIREMENTS.getValue(next)
    fun gap(score: Double?, min: Int): Double = when {
        min == 0 -> 0.0
        score == null -> min.toDouble()
        else -> maxOf(0.0, min - score)
    }
    val gaps = listOf(
        PhysicalAttribute.PHYSIQUE to gap(physique, req.minPhysique),
        PhysicalAttribute.STRENGTH to gap(strength, req.minStrength),
        PhysicalAttribute.CONDITIONING to gap(conditioning, req.minConditioning),
    )
    return gaps.filter { it.second > 0.0 }.maxByOrNull { it.second }?.first
}

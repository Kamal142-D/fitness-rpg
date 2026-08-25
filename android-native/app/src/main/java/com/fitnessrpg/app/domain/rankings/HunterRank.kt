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
): HunterRankResult {
    val confidence = when {
        strengthScore == null -> AssessmentConfidence.LOW
        conditioningScore == null -> AssessmentConfidence.MEDIUM
        else -> AssessmentConfidence.HIGH
    }
    val provisional = confidence != AssessmentConfidence.HIGH

    // Weighted base over the assessed pillars (renormalized), plus weakest penalty.
    val weighted = buildList {
        physiqueScore?.let { add(it to 0.40) }
        strengthScore?.let { add(it to 0.40) }
        conditioningScore?.let { add(it to 0.20) }
    }
    val base = if (weighted.isNotEmpty()) {
        weighted.sumOf { it.first * it.second } / weighted.sumOf { it.second }
    } else {
        0.0
    }
    val known = listOfNotNull(physiqueScore, strengthScore, conditioningScore)
    val weakest = known.minOrNull() ?: 0.0
    val hunterScore = clampScore(base * 0.70 + weakest * 0.30)

    val provisionalCapIndex = PROVISIONAL_MAX_RANK.ordinal

    fun pillarPasses(score: Double?, min: Int, rankIndex: Int): Boolean = when {
        min == 0 -> true
        score == null -> rankIndex <= provisionalCapIndex // unassessed: allowed only up to the provisional cap
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
    val cap = if (provisional) PROVISIONAL_MAX_RANK else Rank.S
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
    // Only ASSESSED pillars can be "limiting". An unassessed pillar (e.g. conditioning
    // not yet measured) is surfaced via the provisional flag, not as the limiter.
    fun gap(score: Double?, min: Int): Double = when {
        min == 0 || score == null -> 0.0
        else -> maxOf(0.0, min - score)
    }
    val gaps = listOf(
        PhysicalAttribute.PHYSIQUE to gap(physique, req.minPhysique),
        PhysicalAttribute.STRENGTH to gap(strength, req.minStrength),
        PhysicalAttribute.CONDITIONING to gap(conditioning, req.minConditioning),
    )
    return gaps.filter { it.second > 0.0 }.maxByOrNull { it.second }?.first
}

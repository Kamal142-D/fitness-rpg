package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore
import com.fitnessrpg.app.domain.rank.scoreToRp

private data class PillarEvidence(
    val score: Double?,
    val confidence: AssessmentConfidence,
    val provisional: Boolean,
)

/** Authoritative V3 calculation. XP, level, streaks and discipline are never inputs. */
fun computeHunterRank(
    physique: PhysiqueRankResult?,
    strength: StrengthRankResult?,
    conditioning: ConditioningRankResult?,
): HunterRankResult {
    val pillars = listOf(
        PillarEvidence(physique?.score, physique?.confidence ?: AssessmentConfidence.LOW, physique?.provisional ?: true),
        PillarEvidence(strength?.score, strength?.confidence ?: AssessmentConfidence.LOW, strength?.provisional ?: true),
        PillarEvidence(conditioning?.score, conditioning?.confidence ?: AssessmentConfidence.LOW, conditioning?.provisional ?: true),
    )
    val known = pillars.filter { it.score != null }
    val missing = known.size != pillars.size
    val confidence = when {
        missing || known.any { it.confidence == AssessmentConfidence.LOW } -> AssessmentConfidence.LOW
        known.all { it.confidence == AssessmentConfidence.HIGH } -> AssessmentConfidence.HIGH
        else -> AssessmentConfidence.MEDIUM
    }
    val provisional = missing || known.any { it.provisional }

    val hunterScore = if (missing) null else {
        val physiqueScore = physique!!.score!!
        val strengthScore = strength!!.score!!
        val conditioningScore = conditioning!!.score!!
        val base = physiqueScore * RankingV3Config.HUNTER_PHYSIQUE_WEIGHT +
            strengthScore * RankingV3Config.HUNTER_STRENGTH_WEIGHT +
            conditioningScore * RankingV3Config.HUNTER_CONDITIONING_WEIGHT
        clampScore(base * RankingV3Config.HUNTER_BASE_WEIGHT +
            minOf(physiqueScore, strengthScore, conditioningScore) * RankingV3Config.HUNTER_WEAKEST_WEIGHT)
    }

    fun meets(candidate: Rank): Boolean {
        val req = RankingV3Config.hunterRequirements.getValue(candidate)
        if (hunterScore == null) {
            if (candidate.ordinal > Rank.C.ordinal) return false
            val knownScores = listOfNotNull(physique?.score, strength?.score, conditioning?.score)
            if (knownScores.isEmpty() || knownScores.average() < req.minHunterScore) return candidate == Rank.E
            return (physique?.score?.let { it >= req.minPhysique } ?: true) &&
                (strength?.score?.let { it >= req.minStrength } ?: true) &&
                (conditioning?.score?.let { it >= req.minConditioning } ?: true)
        }
        val score = hunterScore
        if (score < req.minHunterScore) return false
        return physique!!.score!! >= req.minPhysique &&
            strength!!.score!! >= req.minStrength &&
            conditioning!!.score!! >= req.minConditioning
    }

    var rank = Rank.E
    Rank.entries.forEach { if (meets(it)) rank = it }
    val confidenceCap = when (confidence) {
        AssessmentConfidence.LOW -> Rank.C
        AssessmentConfidence.MEDIUM -> Rank.A
        AssessmentConfidence.HIGH -> Rank.SSS
    }
    val cap = minOf(confidenceCap, if (missing) PROVISIONAL_MAX_RANK else Rank.SSS)
    if (rank.ordinal > cap.ordinal) rank = cap

    val next = Rank.entries.getOrNull(rank.ordinal + 1)
    val nextInfo = next?.let {
        val req = RankingV3Config.hunterRequirements.getValue(it)
        NextRankInfo(it, req.minPhysique, req.minStrength, req.minConditioning, req.minHunterScore)
    }

    return HunterRankResult(
        rank = rank,
        hunterScore = hunterScore,
        physiqueScore = physique?.score,
        strengthScore = strength?.score,
        conditioningScore = conditioning?.score,
        limitingAttribute = next?.let { limitingAttributeFor(it, physique?.score, strength?.score, conditioning?.score) },
        provisional = provisional,
        confidence = confidence,
        nextRank = nextInfo,
        rankCap = cap,
        reasons = buildList {
            addAll(physique?.reasons.orEmpty())
            addAll(strength?.reasons.orEmpty())
            addAll(conditioning?.reasons.orEmpty())
            if (physique?.score == null) add("Physique assessment is incomplete.")
            if (strength?.score == null) add("Strength assessment is incomplete.")
            if (conditioning?.score == null) add("Conditioning assessment is incomplete.")
            if (missing) add("Complete every physical pillar to unlock ranks above C.")
            if (confidence != AssessmentConfidence.HIGH) add("High confidence is required for S, S+, SS and SSS.")
        }.distinct(),
        physique = physique,
        strength = strength,
        conditioning = conditioning,
        rp = hunterScore?.let(::scoreToRp) ?: 0,
    )
}
/** Compatibility entry point for score-only callers and deterministic tests. */
fun computeHunterRank(
    physiqueScore: Double?,
    strengthScore: Double?,
    conditioningScore: Double?,
    evidenceConfidence: AssessmentConfidence? = null,
): HunterRankResult {
    val complete = listOf(physiqueScore, strengthScore, conditioningScore).all { it != null }
    val confidence = evidenceConfidence ?: if (complete) AssessmentConfidence.MEDIUM else AssessmentConfidence.LOW
    fun p(score: Double?) = score?.let { PhysiqueRankResult(it, null, null, null, null, null, null, false, confidence, false, emptyList()) }
    fun s(score: Double?) = score?.let { StrengthRankResult(it, null, emptyMap(), null, false, confidence, emptyList()) }
    fun c(score: Double?) = score?.let { ConditioningRankResult(it, null, false, confidence, emptyList()) }
    return computeHunterRank(p(physiqueScore), s(strengthScore), c(conditioningScore))
}
private fun limitingAttributeFor(next: Rank, physique: Double?, strength: Double?, conditioning: Double?): PhysicalAttribute? {
    val req = RankingV3Config.hunterRequirements.getValue(next)
    fun gap(score: Double?, minimum: Int): Double = if (score == null) minimum.toDouble() else maxOf(0.0, minimum - score)
    return listOf(
        PhysicalAttribute.PHYSIQUE to gap(physique, req.minPhysique),
        PhysicalAttribute.STRENGTH to gap(strength, req.minStrength),
        PhysicalAttribute.CONDITIONING to gap(conditioning, req.minConditioning),
    ).filter { it.second > 0.0 }.maxByOrNull { it.second }?.first
}

package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.clampScore

private data class PillarEvidence(
    val score: Double?,
    val confidence: AssessmentConfidence,
    val provisional: Boolean,
)

/** Authoritative V2 calculation. XP, streaks and discipline never affect Hunter Rank. */
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

    val weighted = buildList {
        physique?.score?.let { add(it to RankingV2Config.HUNTER_PHYSIQUE_WEIGHT) }
        strength?.score?.let { add(it to RankingV2Config.HUNTER_STRENGTH_WEIGHT) }
        conditioning?.score?.let { add(it to RankingV2Config.HUNTER_CONDITIONING_WEIGHT) }
    }
    val hunterScore = weighted.takeIf { it.isNotEmpty() }?.let { values ->
        val base = values.sumOf { it.first * it.second } / values.sumOf { it.second }
        clampScore(base * RankingV2Config.HUNTER_BASE_WEIGHT + values.minOf { it.first } * RankingV2Config.HUNTER_WEAKEST_WEIGHT)
    }

    fun meets(candidate: Rank): Boolean {
        val score = hunterScore ?: return candidate == Rank.E
        val req = RankingV2Config.hunterRequirements.getValue(candidate)
        if (score < req.minHunterScore) return false
        if (candidate.ordinal > PROVISIONAL_MAX_RANK.ordinal && missing) return false
        return (physique?.score == null || physique.score >= req.minPhysique) &&
            (strength?.score == null || strength.score >= req.minStrength) &&
            (conditioning?.score == null || conditioning.score >= req.minConditioning)
    }

    var rank = Rank.E
    Rank.entries.forEach { if (meets(it)) rank = it }
    val confidenceCap = when (confidence) {
        AssessmentConfidence.LOW -> Rank.C
        AssessmentConfidence.MEDIUM -> Rank.A
        AssessmentConfidence.HIGH -> Rank.S
    }
    val cap = minOf(confidenceCap, if (provisional) PROVISIONAL_MAX_RANK else Rank.S)
    if (rank.ordinal > cap.ordinal) rank = cap

    val next = Rank.entries.getOrNull(rank.ordinal + 1)
    val nextInfo = next?.let {
        val req = RankingV2Config.hunterRequirements.getValue(it)
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
            if (provisional) add("Complete current assessments for every physical pillar to unlock B, A and S.")
            if (confidence != AssessmentConfidence.HIGH) add("Recent validated evidence is required for S Rank.")
        }.distinct(),
        physique = physique,
        strength = strength,
        conditioning = conditioning,
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
    val req = RankingV2Config.hunterRequirements.getValue(next)
    fun gap(score: Double?, minimum: Int): Double = if (score == null) minimum.toDouble() else maxOf(0.0, minimum - score)
    return listOf(
        PhysicalAttribute.PHYSIQUE to gap(physique, req.minPhysique),
        PhysicalAttribute.STRENGTH to gap(strength, req.minStrength),
        PhysicalAttribute.CONDITIONING to gap(conditioning, req.minConditioning),
    ).filter { it.second > 0.0 }.maxByOrNull { it.second }?.first
}

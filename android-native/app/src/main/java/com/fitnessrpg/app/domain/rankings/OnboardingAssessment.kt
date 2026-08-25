package com.fitnessrpg.app.domain.rankings

/**
 * Compute a provisional Hunter Rank from onboarding data. Conditioning is never
 * assessed during onboarding, so the result is always provisional (capped at C).
 * If the user skips the strength assessment, [strength] is null and confidence
 * drops to LOW.
 */
fun computeOnboardingHunterRank(
    body: BodyCompositionData,
    strength: List<StrengthAssessmentInput>?,
    conditioning: ConditioningInput? = null,
): HunterRankResult {
    val physiqueScore = computePhysiqueScore(body)
    val strengthScore = strength
        ?.takeIf { it.isNotEmpty() }
        ?.let { computeStrengthScore(it, body.weightKg, body.sex) }
    return computeHunterRank(
        physiqueScore = physiqueScore,
        strengthScore = strengthScore,
        conditioningScore = computeConditioningScore(conditioning),
    )
}

package com.fitnessrpg.app.domain.rankings

/**
 * Derive the rich [HunterRankResult] from stored pillar scores (for display).
 * A 0 score is treated as "not assessed". Because conditioning is not yet stored,
 * this always evaluates as provisional — which also re-gates existing users whose
 * old rank was computed by the previous (weighted-average) system.
 */
fun hunterRankFromScores(
    physiqueScore: Double,
    strengthScore: Double,
    conditioningScore: Double? = null,
): HunterRankResult = computeHunterRank(
    physiqueScore = physiqueScore.takeIf { it > 0.0 },
    strengthScore = strengthScore.takeIf { it > 0.0 },
    conditioningScore = conditioningScore,
)

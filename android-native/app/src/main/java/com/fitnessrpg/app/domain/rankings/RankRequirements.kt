package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank

/**
 * Minimum requirements for each Hunter Rank. A user receives the highest rank for
 * which ALL requirements (overall score AND every assessed pillar) are satisfied.
 * These are the initial, tunable thresholds.
 */
val HUNTER_RANK_REQUIREMENTS: Map<Rank, RankRequirement> = RankingV2Config.hunterRequirements

/** The highest rank reachable while the assessment is provisional (incomplete). */
val PROVISIONAL_MAX_RANK: Rank = Rank.C

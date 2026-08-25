package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank

/**
 * Minimum requirements for each Hunter Rank. A user receives the highest rank for
 * which ALL requirements (overall score AND every assessed pillar) are satisfied.
 * These are the initial, tunable thresholds.
 */
val HUNTER_RANK_REQUIREMENTS: Map<Rank, RankRequirement> = mapOf(
    Rank.E to RankRequirement(minHunterScore = 0, minPhysique = 0, minStrength = 0, minConditioning = 0),
    Rank.D to RankRequirement(minHunterScore = 25, minPhysique = 20, minStrength = 15, minConditioning = 15),
    Rank.C to RankRequirement(minHunterScore = 40, minPhysique = 35, minStrength = 30, minConditioning = 25),
    Rank.B to RankRequirement(minHunterScore = 55, minPhysique = 50, minStrength = 45, minConditioning = 40),
    Rank.A to RankRequirement(minHunterScore = 72, minPhysique = 68, minStrength = 68, minConditioning = 60),
    Rank.S to RankRequirement(minHunterScore = 87, minPhysique = 82, minStrength = 82, minConditioning = 78),
)

/** The highest rank reachable while the assessment is provisional (incomplete). */
val PROVISIONAL_MAX_RANK: Rank = Rank.C

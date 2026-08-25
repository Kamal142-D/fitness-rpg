package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rankings.RankingV2Config

/**
 * Ranking configuration — ALL tunable constants live here, isolated from logic
 * and UI (PLAN.txt §6). Values marked PROVISIONAL are seed estimates. Bump
 * [RANKING_VERSION] when the math or these constants change.
 */
const val RANKING_VERSION = RankingV2Config.VERSION

/** Gate Score component weights (PLAN.txt §6.5). Must sum to 1. */
object GateWeights {
    const val PERFORMANCE = RankingV2Config.CLEAR_TARGET_WEIGHT
    const val COMPLETION = RankingV2Config.CLEAR_COMPLETION_WEIGHT
    const val PROGRESS = RankingV2Config.CLEAR_PROGRESS_WEIGHT
    const val PR = RankingV2Config.CLEAR_PR_WEIGHT
    const val QUALITY = 0.0
}

/** Neutral score used where a factor has no data yet (don't punish new users). */
const val NEUTRAL_SCORE = 60.0

/** Plausibility bounds for anti-inflation validation (PLAN.txt §6.6). */
object ValidationLimits {
    const val MIN_WEIGHT_KG = 0.0
    const val MAX_WEIGHT_KG = 600.0
    const val MIN_REPS = 1
    const val MAX_REPS = 100
    const val MIN_RPE = 0.0
    const val MAX_RPE = 10.0

    /** A qualifying performance needs at least this many valid working sets. */
    const val MIN_QUALIFYING_SETS = 2

    /** Reaching S rank must be demonstrated across at least this many sessions. */
    const val MIN_SESSIONS_FOR_S = 2

    /** A single rank update may not jump more than this many bands. */
    const val MAX_RANK_JUMP = 2
}

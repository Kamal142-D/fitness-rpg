package com.fitnessrpg.app.domain.progression

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * XP curve and helpers (PLAN.txt §6.10). Pure and deterministic. Level tracks
 * activity/account progression (distinct from Hunter rank).
 */

/** XP required to advance FROM the given level to the next: round(100 * level^1.5). */
fun getXpRequiredForLevel(level: Int): Int {
    val l = maxOf(1, level)
    return (100 * l.toDouble().pow(1.5)).roundToLong().toInt()
}

data class XpProgress(
    /** XP earned toward the current level (clamped to [0, required]). */
    val current: Int,
    /** XP needed to reach the next level. */
    val required: Int,
    /** current / required, in [0, 1]. */
    val fraction: Double,
)

/** Progress of [currentXp] within [level]. */
fun xpProgress(currentXp: Int, level: Int): XpProgress {
    val required = getXpRequiredForLevel(level)
    val current = maxOf(0, minOf(currentXp, required))
    return XpProgress(current, required, if (required > 0) current.toDouble() / required else 0.0)
}

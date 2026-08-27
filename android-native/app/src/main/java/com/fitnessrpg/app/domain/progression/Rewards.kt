package com.fitnessrpg.app.domain.progression

import com.fitnessrpg.app.domain.rank.Rank

/** XP rewards + level application (PLAN.txt §6.10). Pure. */
object XpRewards {
    const val COMPLETE_WORKOUT = 300
    const val PER_VALID_WORKING_SET = 10
    const val PER_MEANINGFUL_PR = 50

    /** Gate clear bonus by clear rank (within the documented 100-300 band). */
    val GATE_CLEAR_BONUS: Map<Rank, Int> = mapOf(
        Rank.E to 100,
        Rank.D to 100,
        Rank.C to 150,
        Rank.B to 200,
        Rank.A to 250,
        Rank.S to 300,
        Rank.S_PLUS to 350,
        Rank.SS to 400,
        Rank.SSS to 500,
    )
}

data class WorkoutXpInput(
    val completed: Boolean,
    val validWorkingSets: Int,
    val meaningfulPrCount: Int,
    val gateClearRank: Rank?,
)

/** Total XP earned from a finished workout. */
fun xpForWorkout(input: WorkoutXpInput): Int {
    var xp = 0
    if (input.completed) xp += XpRewards.COMPLETE_WORKOUT
    xp += maxOf(0, input.validWorkingSets) * XpRewards.PER_VALID_WORKING_SET
    xp += maxOf(0, input.meaningfulPrCount) * XpRewards.PER_MEANINGFUL_PR
    input.gateClearRank?.let { xp += XpRewards.GATE_CLEAR_BONUS.getValue(it) }
    return xp
}

data class ProgressionSnapshot(val level: Int, val currentXp: Int, val lifetimeXp: Int)

data class XpApplication(
    val level: Int,
    val currentXp: Int,
    val lifetimeXp: Int,
    val leveledUp: Boolean,
    val levelsGained: Int,
)

/**
 * Apply earned XP to a progression snapshot, rolling over multiple levels if the
 * gain is large. Deterministic.
 */
fun applyXp(current: ProgressionSnapshot, earned: Int): XpApplication {
    val gain = maxOf(0, earned)
    var level = maxOf(1, current.level)
    var currentXp = current.currentXp + gain
    var levelsGained = 0

    // Guard against pathological input with a hard cap on rollovers.
    while (currentXp >= getXpRequiredForLevel(level) && levelsGained < 1000) {
        currentXp -= getXpRequiredForLevel(level)
        level += 1
        levelsGained += 1
    }

    return XpApplication(
        level = level,
        currentXp = currentXp,
        lifetimeXp = current.lifetimeXp + gain,
        leveledUp = levelsGained > 0,
        levelsGained = levelsGained,
    )
}

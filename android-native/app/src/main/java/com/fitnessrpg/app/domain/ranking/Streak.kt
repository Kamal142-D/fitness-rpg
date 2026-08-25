package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.clampScore

/**
 * Streak, adherence, and the Discipline attribute (PLAN.txt §6.9).
 *
 * Rest is respected: a scheduled rest day never breaks a streak, and Discipline
 * does not reward unsafe "never miss a day" behavior (the streak contribution is
 * capped).
 */
data class StreakState(val current: Int, val longest: Int)

data class DayOutcome(
    val didTrain: Boolean,
    /** A planned rest day — must not break the streak. */
    val isScheduledRest: Boolean,
)

/** Advance a streak by one day's outcome. */
fun updateStreak(prev: StreakState, day: DayOutcome): StreakState {
    if (day.didTrain) {
        val current = prev.current + 1
        return StreakState(current, maxOf(prev.longest, current))
    }
    if (day.isScheduledRest) {
        return prev // rest day: streak preserved
    }
    return StreakState(0, prev.longest) // missed a planned training day
}

/** Adherence over a window: completed / planned, clamped to 0..1. */
fun adherenceRatio(completedWorkouts: Int, plannedWorkouts: Int): Double {
    if (plannedWorkouts <= 0) return 0.0
    return maxOf(0.0, minOf(1.0, completedWorkouts.toDouble() / plannedWorkouts))
}

data class DisciplineInput(
    val currentStreakDays: Int,
    /** 0..1 adherence over a recent window. */
    val adherence: Double,
)

/**
 * Discipline attribute: mostly adherence, with a capped streak bonus so a long
 * streak can't dominate and unsafe behavior isn't incentivized.
 */
fun disciplineScore(input: DisciplineInput): Double {
    val adherencePart = maxOf(0.0, minOf(1.0, input.adherence)) * 75.0
    val streakBonus = minOf(25.0, maxOf(0.0, input.currentStreakDays.toDouble()) * 2.5)
    return clampScore(adherencePart + streakBonus)
}

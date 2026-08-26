package com.fitnessrpg.app.domain.steps

import java.time.LocalDate
import kotlin.math.roundToInt

const val DEFAULT_DAILY_STEP_GOAL = 8_000
const val DAILY_MARCH_REWARD_XP = 100

enum class StepSource { HEALTH_CONNECT, DEVICE_SENSOR, NONE }

data class DailyStepProgress(
    val date: LocalDate,
    val steps: Int,
    val goal: Int,
    val rewardClaimed: Boolean = false,
    val xpAwarded: Int = 0,
) {
    val completed: Boolean get() = steps >= goal
    val fraction: Float get() = stepGoalFraction(steps, goal)
}

data class SensorAccumulator(
    val date: LocalDate,
    val lastBootStepCount: Long? = null,
    val accumulatedSteps: Int = 0,
)

/**
 * Converts Android's since-boot step counter into a conservative daily delta.
 * The first sample of a new day establishes a baseline rather than assigning
 * overnight steps to the wrong date. Sensor resets (device reboot) are also
 * treated as a new baseline.
 */
fun accumulateSensorSample(
    current: SensorAccumulator,
    bootStepCount: Long,
    today: LocalDate,
): SensorAccumulator {
    val sample = bootStepCount.coerceAtLeast(0L)
    if (current.date != today) return SensorAccumulator(today, sample, 0)
    val previous = current.lastBootStepCount ?: return current.copy(lastBootStepCount = sample)
    if (sample < previous) return current.copy(lastBootStepCount = sample)
    val delta = (sample - previous).coerceAtMost(100_000L).toInt()
    return current.copy(
        lastBootStepCount = sample,
        accumulatedSteps = (current.accumulatedSteps + delta).coerceIn(0, 100_000),
    )
}

fun stepGoalFraction(steps: Int, goal: Int): Float =
    if (goal <= 0) 0f else (steps.coerceAtLeast(0).toFloat() / goal).coerceIn(0f, 1f)

/** Approximation only; no GPS or stride-profile data is collected. */
fun estimatedDistanceKm(steps: Int, strideMeters: Double = .75): Double =
    steps.coerceAtLeast(0) * strideMeters.coerceIn(.3, 1.5) / 1_000.0

/** A simple activity estimate based on roughly 100 walking steps per minute. */
fun estimatedActiveMinutes(steps: Int): Int = (steps.coerceAtLeast(0) / 100.0).roundToInt()

/** Consecutive completed days ending today, or yesterday when today is open. */
fun completedStepStreak(days: Collection<DailyStepProgress>, today: LocalDate): Int {
    val completedDates = days.filter { it.completed }.map { it.date }.toSet()
    var cursor = if (today in completedDates) today else today.minusDays(1)
    var streak = 0
    while (cursor in completedDates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

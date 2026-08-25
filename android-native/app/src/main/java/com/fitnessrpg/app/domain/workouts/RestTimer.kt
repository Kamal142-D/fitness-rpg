package com.fitnessrpg.app.domain.workouts

import kotlin.math.ceil

/**
 * Rest-timer math. Pure and time-injectable so it survives app restarts: the
 * store persists an absolute [restEndsAt] epoch and the remaining time is always
 * derived from the current clock, never a decrementing counter.
 */
fun restRemainingSeconds(restEndsAt: Long?, now: Long = System.currentTimeMillis()): Int {
    if (restEndsAt == null) return 0
    val ms = restEndsAt - now
    return if (ms <= 0) 0 else ceil(ms / 1000.0).toInt()
}

fun isResting(restEndsAt: Long?, now: Long = System.currentTimeMillis()): Boolean =
    restRemainingSeconds(restEndsAt, now) > 0

/** Format seconds as M:SS. */
fun formatClock(totalSeconds: Int): String {
    val s = maxOf(0, totalSeconds)
    val m = s / 60
    val rem = s % 60
    return "$m:${rem.toString().padStart(2, '0')}"
}

package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.util.round2

/**
 * Estimated 1RM via the Epley formula (PLAN.txt §6.2): weight * (1 + reps / 30).
 * Only sensible for conventional loaded reps in the 1..12 range — returns null
 * otherwise so callers don't store noise.
 */
fun estimatedOneRepMax(weightKg: Double?, reps: Int?): Double? {
    if (weightKg == null || reps == null) return null
    if (weightKg <= 0.0 || reps < 1 || reps > 12) return null
    if (reps == 1) return round2(weightKg)
    return round2(weightKg * (1 + reps / 30.0))
}

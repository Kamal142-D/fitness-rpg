package com.fitnessrpg.app.domain.rankings

/**
 * Estimated 1RM via Epley: weight * (1 + reps / 30). Primarily meaningful for
 * 1–12 reps; callers should NOT feed high-rep sets in to inflate strength (the
 * strength scorer clamps reps for ranking).
 */
fun calculateEstimated1RM(weightKg: Double, reps: Int): Double {
    if (weightKg <= 0.0 || reps < 1) return 0.0
    if (reps == 1) return weightKg
    return weightKg * (1.0 + reps / 30.0)
}

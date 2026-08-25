package com.fitnessrpg.app.domain.rankings

/**
 * Conditioning (cardiovascular fitness / work capacity). No conditioning
 * assessment exists in the app yet, so this returns null (UNKNOWN) rather than
 * inventing a score. An unknown conditioning value forces a provisional rank and
 * blocks A/S until a real assessment (run test, HR recovery, work capacity) lands.
 */
data class ConditioningInput(
    /** Placeholder for a future conditioning assessment; null = not assessed. */
    val workCapacityScore: Double? = null,
)

fun computeConditioningScore(input: ConditioningInput?): Double? = input?.workCapacityScore

package com.fitnessrpg.app.domain.ranking

/**
 * Anti-inflation validation (PLAN.txt §6.6). Pure. Raw logs are never modified —
 * these helpers only decide which sets QUALIFY for ranking and explain why.
 */
data class RankingSetInput(
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
)

data class SetValidation(
    val valid: Boolean,
    /** Why the set was excluded, for user-facing explanation. Null when valid. */
    val reason: String?,
)

/**
 * A set qualifies for strength ranking when it is a completed working set with
 * plausible, loaded weight and reps in the strength range (1..12). Warm-ups,
 * bodyweight-only (no load), and implausible values are excluded.
 */
fun validateWorkingSet(set: RankingSetInput): SetValidation {
    if (!set.isCompleted) return SetValidation(false, "Set was not completed")
    if (set.isWarmup) return SetValidation(false, "Warm-up sets do not count")

    val weight = set.weightKg
    if (weight == null || weight <= ValidationLimits.MIN_WEIGHT_KG) {
        return SetValidation(false, "No load recorded")
    }
    if (weight > ValidationLimits.MAX_WEIGHT_KG) {
        return SetValidation(false, "Weight is implausibly high")
    }
    val reps = set.reps
    if (reps == null || reps < ValidationLimits.MIN_REPS) {
        return SetValidation(false, "No reps recorded")
    }
    if (reps > ValidationLimits.MAX_REPS) {
        return SetValidation(false, "Reps are implausibly high")
    }
    if (reps > 12) {
        return SetValidation(false, "Reps above 12 are not used for strength ranking")
    }
    val rpe = set.rpe
    if (rpe != null && (rpe < ValidationLimits.MIN_RPE || rpe > ValidationLimits.MAX_RPE)) {
        return SetValidation(false, "RPE is out of range")
    }
    return SetValidation(true, null)
}

/** The subset of sets that qualify for ranking. */
fun <T : RankingSetInput> qualifyingWorkingSets(sets: List<T>): List<T> =
    sets.filter { validateWorkingSet(it).valid }

/**
 * Whether a set of qualifying sets is enough to rank a performance at all
 * (PLAN.txt §6.6: need >= 2 valid sets, don't rank from one implausible set).
 */
fun meetsQualifyingThreshold(qualifyingCount: Int): Boolean =
    qualifyingCount >= ValidationLimits.MIN_QUALIFYING_SETS

package com.fitnessrpg.app.domain.onboarding

import java.time.LocalDate
import java.time.ZoneOffset

/** Per-field validation messages, keyed by the draft field name. */
typealias FieldErrors = Map<String, String>

enum class StepId {
    WELCOME, IDENTITY, MEASUREMENTS, GOAL, EXPERIENCE, SCHEDULE, DETAILS, REVEAL
}

/** Steps that require validation before advancing. */
val VALIDATED_STEPS: List<StepId> = listOf(
    StepId.IDENTITY, StepId.MEASUREMENTS, StepId.GOAL,
    StepId.EXPERIENCE, StepId.SCHEDULE, StepId.DETAILS,
)

private val DATE_RE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** True for a real calendar date in strict YYYY-MM-DD form. */
fun isValidDateString(value: String): Boolean {
    if (!DATE_RE.matches(value)) return false
    return try {
        val (y, m, d) = value.split("-").map { it.toInt() }
        val date = LocalDate.of(y, m, d)
        date.year == y && date.monthValue == m && date.dayOfMonth == d
    } catch (e: Exception) {
        false
    }
}

/** Whole-year age from a YYYY-MM-DD date of birth, relative to [now] (UTC). */
fun ageFromDob(dob: String, now: LocalDate = LocalDate.now(ZoneOffset.UTC)): Int {
    val (y, m, d) = dob.split("-").map { it.toInt() }
    var age = now.year - y
    val beforeBirthday = now.monthValue < m || (now.monthValue == m && now.dayOfMonth < d)
    if (beforeBirthday) age -= 1
    return age
}

private fun inRange(v: Double?, lo: Double, hi: Double): Boolean = v != null && v >= lo && v <= hi
private fun inRange(v: Int?, lo: Int, hi: Int): Boolean = v != null && v >= lo && v <= hi

/** Validate the fields owned by a given step. Returns per-field messages. */
fun validateStep(step: StepId, d: OnboardingDraft): FieldErrors {
    val e = mutableMapOf<String, String>()
    when (step) {
        StepId.IDENTITY -> {
            if (d.displayName.trim().isEmpty()) e["displayName"] = "Enter a name"
            else if (d.displayName.trim().length > 40) e["displayName"] = "Keep it under 40 characters"
            if (d.dateOfBirth.isEmpty()) {
                e["dateOfBirth"] = "Enter your date of birth"
            } else if (!isValidDateString(d.dateOfBirth)) {
                e["dateOfBirth"] = "Use the format YYYY-MM-DD"
            } else {
                val age = ageFromDob(d.dateOfBirth)
                if (age < 13 || age > 100) e["dateOfBirth"] = "Age must be between 13 and 100"
            }
            if (d.sex == null) e["sex"] = "Select an option"
        }

        StepId.MEASUREMENTS -> {
            if (!inRange(d.heightCm, 100.0, 250.0)) e["heightCm"] = "Enter a height between 100 and 250 cm"
            if (!inRange(d.currentWeightKg, 30.0, 300.0)) e["currentWeightKg"] = "Enter a weight between 30 and 300 kg"
        }

        StepId.GOAL -> {
            if (d.fitnessGoal == null) e["fitnessGoal"] = "Choose a goal"
        }

        StepId.EXPERIENCE -> {
            if (d.experienceLevel == null) e["experienceLevel"] = "Choose your experience level"
        }

        StepId.SCHEDULE -> {
            if (!inRange(d.trainingDaysPerWeek, 1, 7)) e["trainingDaysPerWeek"] = "Choose how many days you can train"
            if (d.trainingLocation == null) e["trainingLocation"] = "Choose where you train"
            if (!inRange(d.preferredWorkoutMinutes, 10, 240)) e["preferredWorkoutMinutes"] = "Enter a duration between 10 and 240 minutes"
        }

        StepId.DETAILS -> {
            if (d.bodyFatPercent != null && !inRange(d.bodyFatPercent, 3.0, 60.0)) {
                e["bodyFatPercent"] = "Body fat should be between 3 and 60%"
            }
            if (d.waistCm != null && !inRange(d.waistCm, 40.0, 200.0)) e["waistCm"] = "Waist should be between 40 and 200 cm"
            if (d.skeletalMuscleMassKg != null && d.currentWeightKg != null && d.skeletalMuscleMassKg > d.currentWeightKg) {
                e["skeletalMuscleMassKg"] = "Skeletal muscle mass cannot exceed body weight"
            }
            if (d.skeletalMuscleMassKg != null && !inRange(d.skeletalMuscleMassKg, 10.0, 80.0)) {
                e["skeletalMuscleMassKg"] = "Muscle mass should be between 10 and 80 kg"
            }
            val lifts = mapOf(
                "baselineBenchKg" to d.baselineBenchKg,
                "baselineSquatKg" to d.baselineSquatKg,
                "baselineDeadliftKg" to d.baselineDeadliftKg,
            )
            for ((key, v) in lifts) {
                if (v != null && !inRange(v, 1.0, 500.0)) e[key] = "Enter a value between 1 and 500 kg"
            }
        }

        else -> Unit
    }
    return e
}

fun hasErrors(errors: FieldErrors): Boolean = errors.isNotEmpty()

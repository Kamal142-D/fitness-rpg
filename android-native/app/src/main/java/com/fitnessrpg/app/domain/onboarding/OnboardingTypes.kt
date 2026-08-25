package com.fitnessrpg.app.domain.onboarding

import com.fitnessrpg.app.domain.rankings.StrengthAssessmentInput

enum class Sex(val wire: String) {
    MALE("male"), FEMALE("female"), INTERSEX("intersex"), PREFER_NOT_TO_SAY("prefer_not_to_say")
}

enum class ExperienceLevel(val wire: String) {
    BEGINNER("beginner"), INTERMEDIATE("intermediate"), ADVANCED("advanced")
}

enum class FitnessGoal(val wire: String) {
    BUILD_MUSCLE("build_muscle"),
    LOSE_FAT("lose_fat"),
    GET_STRONGER("get_stronger"),
    GENERAL_FITNESS("general_fitness"),
    IMPROVE_ENDURANCE("improve_endurance"),
}

enum class TrainingLocation(val wire: String) { GYM("gym"), HOME("home") }

/**
 * The onboarding draft: everything collected across the Awakening steps before
 * it is persisted. Numeric fields are null until entered.
 */
data class OnboardingDraft(
    val displayName: String = "",
    val dateOfBirth: String = "", // YYYY-MM-DD
    val sex: Sex? = null,
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val fitnessGoal: FitnessGoal? = null,
    val experienceLevel: ExperienceLevel? = null,
    val trainingDaysPerWeek: Int? = null,
    val trainingLocation: TrainingLocation? = null,
    val preferredWorkoutMinutes: Int? = null,
    val bodyFatPercent: Double? = null,
    val waistCm: Double? = null,
    val bodyAssessmentSource: String = "manual",
    val conditioningTestType: String? = null,
    val conditioningResult: Double? = null,
    val strengthAssessmentSets: List<StrengthAssessmentInput> = emptyList(),
    val skeletalMuscleMassKg: Double? = null,
    val baselineBenchKg: Double? = null,
    val baselineSquatKg: Double? = null,
    val baselineDeadliftKg: Double? = null,
)

/** A fresh, empty draft. */
fun emptyDraft(): OnboardingDraft = OnboardingDraft()

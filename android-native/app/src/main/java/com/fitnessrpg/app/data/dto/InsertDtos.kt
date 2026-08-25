package com.fitnessrpg.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Insert/update bodies for the tables the client writes to directly. */

@Serializable
data class WorkoutTemplateInsertDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    val difficulty: String? = null,
    @SerialName("is_system_template") val isSystemTemplate: Boolean,
    @SerialName("estimated_duration_minutes") val estimatedDurationMinutes: Int,
    val description: String,
)

@Serializable
data class TemplateExerciseInsertDto(
    @SerialName("template_id") val templateId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("target_sets") val targetSets: Int,
    @SerialName("target_reps_min") val targetRepsMin: Int,
    @SerialName("target_reps_max") val targetRepsMax: Int,
    @SerialName("target_rpe") val targetRpe: Int,
    @SerialName("rest_seconds") val restSeconds: Int,
)

@Serializable
data class WorkoutTemplateNameUpdateDto(
    val name: String,
    @SerialName("estimated_duration_minutes") val estimatedDurationMinutes: Int,
)

@Serializable
data class ProfileOnboardingUpdateDto(
    @SerialName("display_name") val displayName: String?,
    @SerialName("date_of_birth") val dateOfBirth: String?,
    val sex: String?,
    @SerialName("height_cm") val heightCm: Double?,
    @SerialName("current_weight_kg") val currentWeightKg: Double?,
    @SerialName("experience_level") val experienceLevel: String?,
    @SerialName("fitness_goal") val fitnessGoal: String?,
    @SerialName("training_days_per_week") val trainingDaysPerWeek: Int?,
    @SerialName("training_location") val trainingLocation: String?,
    @SerialName("preferred_workout_minutes") val preferredWorkoutMinutes: Int?,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
)

@Serializable
data class ProfilePhysicalUpdateDto(
    @SerialName("height_cm") val heightCm: Double,
    @SerialName("current_weight_kg") val currentWeightKg: Double,
)

@Serializable
data class ProgressionInitUpdateDto(
    @SerialName("strength_score") val strengthScore: Double,
    @SerialName("physique_score") val physiqueScore: Double,
    @SerialName("endurance_score") val enduranceScore: Double,
    @SerialName("conditioning_score") val conditioningScore: Double?,
    @SerialName("discipline_score") val disciplineScore: Double,
    @SerialName("hunter_score") val hunterScore: Double,
    @SerialName("hunter_rank") val hunterRank: String,
    @SerialName("hunter_rank_provisional") val hunterRankProvisional: Boolean,
    @SerialName("hunter_rank_confidence") val hunterRankConfidence: String,
    @SerialName("hunter_rank_cap") val hunterRankCap: String?,
    @SerialName("hunter_rank_reasons") val hunterRankReasons: List<String>,
    @SerialName("assessment_update_required") val assessmentUpdateRequired: Boolean,
)

@Serializable
data class BodyAssessmentInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("weight_kg") val weightKg: Double?,
    @SerialName("body_fat_percent") val bodyFatPercent: Double?,
    @SerialName("skeletal_muscle_mass_kg") val skeletalMuscleMassKg: Double?,
    @SerialName("waist_cm") val waistCm: Double?,
    @SerialName("lean_body_mass_kg") val leanBodyMassKg: Double? = null,
    @SerialName("left_arm_lean_mass_kg") val leftArmLeanMassKg: Double? = null,
    @SerialName("right_arm_lean_mass_kg") val rightArmLeanMassKg: Double? = null,
    @SerialName("left_leg_lean_mass_kg") val leftLegLeanMassKg: Double? = null,
    @SerialName("right_leg_lean_mass_kg") val rightLegLeanMassKg: Double? = null,
    val source: String,
)

@Serializable
data class ConditioningAssessmentInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("test_type") val testType: String,
    val result: Double,
    val score: Double?,
)

@Serializable
data class StrengthAssessmentSetInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_id") val exerciseId: String,
    val variation: String,
    val equipment: String,
    @SerialName("weight_kg") val weightKg: Double,
    val reps: Int,
    @SerialName("weight_mode") val weightMode: String?,
    val rpe: Double?,
)

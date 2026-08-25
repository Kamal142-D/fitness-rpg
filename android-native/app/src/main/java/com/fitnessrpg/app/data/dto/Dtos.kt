package com.fitnessrpg.app.data.dto

import com.fitnessrpg.app.domain.analytics.ExerciseStatInput
import com.fitnessrpg.app.domain.analytics.PrHistoryItem
import com.fitnessrpg.app.domain.analytics.SessionSummary
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.domain.model.TemplateExercise
import com.fitnessrpg.app.domain.pr.PriorStat
import com.fitnessrpg.app.domain.rank.rankOrDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data-transfer objects mirroring Supabase table columns (snake_case), each with
 * a mapper to a clean domain model. Fields not always selected default to null.
 */

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String? = null,
    @SerialName("primary_muscle_group") val primaryMuscleGroup: String? = null,
    @SerialName("secondary_muscle_groups") val secondaryMuscleGroups: List<String>? = null,
    val equipment: String? = null,
    @SerialName("exercise_type") val exerciseType: String? = null,
    @SerialName("ranking_enabled") val rankingEnabled: Boolean = false,
    val aliases: List<String>? = null,
    @SerialName("body_part") val bodyPart: String? = null,
    @SerialName("target_muscle") val targetMuscle: String? = null,
    val instructions: List<String>? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    val source: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    val attribution: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain() = Exercise(
        id = id, name = name, category = category, primaryMuscleGroup = primaryMuscleGroup,
        secondaryMuscleGroups = secondaryMuscleGroups ?: emptyList(), equipment = equipment,
        exerciseType = exerciseType, rankingEnabled = rankingEnabled, createdAt = createdAt,
        aliases = aliases ?: emptyList(), bodyPart = bodyPart, targetMuscle = targetMuscle,
        instructions = instructions ?: emptyList(), imageUrl = imageUrl, videoUrl = videoUrl,
        source = source, sourceId = sourceId, attribution = attribution,
    )
}

@Serializable
data class WorkoutTemplateDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("estimated_duration_minutes") val estimatedDurationMinutes: Int? = null,
    val difficulty: String? = null,
    @SerialName("is_system_template") val isSystemTemplate: Boolean = false,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("last_difficulty_score") val lastDifficultyScore: Double? = null,
    @SerialName("last_difficulty_rank") val lastDifficultyRank: String? = null,
    @SerialName("average_difficulty_score") val averageDifficultyScore: Double? = null,
    @SerialName("average_difficulty_rank") val averageDifficultyRank: String? = null,
    @SerialName("times_completed") val timesCompleted: Int = 0,
    @SerialName("last_completed_at") val lastCompletedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain() = GateTemplate(
        id = id, userId = userId, name = name, description = description,
        estimatedDurationMinutes = estimatedDurationMinutes, difficulty = difficulty,
        isSystemTemplate = isSystemTemplate, createdAt = createdAt, updatedAt = updatedAt,
        deletedAt = deletedAt, lastDifficultyScore = lastDifficultyScore,
        lastDifficultyRank = lastDifficultyRank, averageDifficultyScore = averageDifficultyScore,
        averageDifficultyRank = averageDifficultyRank, timesCompleted = timesCompleted,
        lastCompletedAt = lastCompletedAt,
    )
}

@Serializable
data class TemplateExerciseDto(
    val id: String,
    @SerialName("template_id") val templateId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("target_sets") val targetSets: Int? = null,
    @SerialName("target_reps_min") val targetRepsMin: Int? = null,
    @SerialName("target_reps_max") val targetRepsMax: Int? = null,
    @SerialName("target_rpe") val targetRpe: Double? = null,
    @SerialName("rest_seconds") val restSeconds: Int? = null,
) {
    fun toDomain() = TemplateExercise(
        id = id, templateId = templateId, exerciseId = exerciseId, orderIndex = orderIndex,
        targetSets = targetSets, targetRepsMin = targetRepsMin, targetRepsMax = targetRepsMax,
        targetRpe = targetRpe, restSeconds = restSeconds,
    )
}

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val sex: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("current_weight_kg") val currentWeightKg: Double? = null,
    @SerialName("experience_level") val experienceLevel: String? = null,
    @SerialName("fitness_goal") val fitnessGoal: String? = null,
    @SerialName("training_days_per_week") val trainingDaysPerWeek: Int? = null,
    @SerialName("training_location") val trainingLocation: String? = null,
    @SerialName("preferred_workout_minutes") val preferredWorkoutMinutes: Int? = null,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
) {
    fun toDomain() = Profile(
        id = id, displayName = displayName, dateOfBirth = dateOfBirth, sex = sex,
        heightCm = heightCm, currentWeightKg = currentWeightKg, experienceLevel = experienceLevel,
        fitnessGoal = fitnessGoal, trainingDaysPerWeek = trainingDaysPerWeek,
        trainingLocation = trainingLocation, preferredWorkoutMinutes = preferredWorkoutMinutes,
        onboardingCompleted = onboardingCompleted,
    )
}

@Serializable
data class ProgressionDto(
    @SerialName("user_id") val userId: String,
    val level: Int = 1,
    @SerialName("current_xp") val currentXp: Int = 0,
    @SerialName("lifetime_xp") val lifetimeXp: Int = 0,
    @SerialName("strength_score") val strengthScore: Double = 0.0,
    @SerialName("physique_score") val physiqueScore: Double = 0.0,
    @SerialName("endurance_score") val enduranceScore: Double = 0.0,
    @SerialName("discipline_score") val disciplineScore: Double = 0.0,
    @SerialName("hunter_score") val hunterScore: Double = 60.0,
    @SerialName("hunter_rank") val hunterRank: String? = null,
    @SerialName("current_streak_days") val currentStreakDays: Int = 0,
    @SerialName("longest_streak_days") val longestStreakDays: Int = 0,
    @SerialName("hunter_rank_provisional") val hunterRankProvisional: Boolean = true,
    @SerialName("hunter_rank_confidence") val hunterRankConfidence: String = "low",
    @SerialName("hunter_rank_cap") val hunterRankCap: String? = "C",
    @SerialName("hunter_rank_reasons") val hunterRankReasons: List<String> = emptyList(),
    @SerialName("assessment_update_required") val assessmentUpdateRequired: Boolean = false,
) {
    fun toDomain() = PlayerProgression(
        level = level, currentXp = currentXp, lifetimeXp = lifetimeXp,
        strengthScore = strengthScore, physiqueScore = physiqueScore,
        enduranceScore = enduranceScore, disciplineScore = disciplineScore,
        hunterScore = hunterScore, hunterRank = rankOrDefault(hunterRank),
        currentStreakDays = currentStreakDays, longestStreakDays = longestStreakDays,
        hunterRankProvisional = hunterRankProvisional,
        hunterRankConfidence = hunterRankConfidence,
        hunterRankCap = com.fitnessrpg.app.domain.rank.rankOrNull(hunterRankCap),
        hunterRankReasons = hunterRankReasons,
        assessmentUpdateRequired = assessmentUpdateRequired,
    )
}

@Serializable
data class ExerciseUserStatDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("best_weight_kg") val bestWeightKg: Double? = null,
    @SerialName("best_reps") val bestReps: Double? = null,
    @SerialName("best_estimated_1rm_kg") val bestEstimated1rmKg: Double? = null,
    @SerialName("best_volume_kg") val bestVolumeKg: Double? = null,
) {
    fun toPriorStat() = PriorStat(bestWeightKg, bestReps, bestEstimated1rmKg, bestVolumeKg)
}

@Serializable
data class WorkoutSessionDto(
    val id: String,
    val name: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("gate_clear_rank") val gateClearRank: String? = null,
    @SerialName("total_volume_kg") val totalVolumeKg: Double? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val status: String? = null,
) {
    fun toSummary() = SessionSummary(id, name, completedAt, gateClearRank, totalVolumeKg, durationSeconds)
}

@Serializable
data class PersonalRecordDto(
    val id: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("record_type") val recordType: String,
    @SerialName("new_value") val newValue: Double,
    @SerialName("achieved_at") val achievedAt: String,
) {
    fun toHistory(exerciseName: String) = PrHistoryItem(id, exerciseName, recordType, newValue, achievedAt)
}

@Serializable
data class BodyAssessmentDto(
    @SerialName("body_fat_percent") val bodyFatPercent: Double? = null,
    @SerialName("skeletal_muscle_mass_kg") val skeletalMuscleMassKg: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("assessment_date") val assessmentDate: String? = null,
)

@Serializable
data class QuestDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val type: String,
    @SerialName("requirement_value") val requirementValue: Double,
    @SerialName("xp_reward") val xpReward: Int,
)

@Serializable
data class UserQuestDto(
    val id: String,
    @SerialName("quest_id") val questId: String,
    val progress: Double = 0.0,
    val completed: Boolean = false,
    val claimed: Boolean = false,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("assigned_at") val assignedAt: String? = null,
)

/** Small helper for decoding an inserted row's id. */
@Serializable
data class IdDto(val id: String)

/** Helper for the exercise catalog id->name lookups. */
@Serializable
data class ExerciseNameDto(val id: String, val name: String)

/** Helper for profile sex + bodyweight lookups. */
@Serializable
data class ProfileBasicsDto(
    val sex: String? = null,
    @SerialName("current_weight_kg") val currentWeightKg: Double? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
)

/** Helper for exercise-stats used to compute finish inputs. */
@Serializable
data class StatBestDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("best_estimated_1rm_kg") val bestEstimated1rmKg: Double? = null,
)

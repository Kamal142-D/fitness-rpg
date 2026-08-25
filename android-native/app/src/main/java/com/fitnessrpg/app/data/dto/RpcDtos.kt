package com.fitnessrpg.app.data.dto

import com.fitnessrpg.app.domain.pr.DetectedPR
import com.fitnessrpg.app.domain.pr.NewStat
import com.fitnessrpg.app.domain.progression.ProgressionPersistPayload
import com.fitnessrpg.app.domain.workouts.CompletionExercisePayload
import com.fitnessrpg.app.domain.workouts.CompletionPayload
import com.fitnessrpg.app.domain.workouts.CompletionSession
import com.fitnessrpg.app.domain.workouts.CompletionSetPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable request bodies for the SECURITY DEFINER RPCs. Column names are
 * snake_case to match the jsonb the SQL functions expect. Each is built from the
 * pure domain payloads (which stay camelCase).
 */

@Serializable
data class CompletionSetDto(
    @SerialName("set_number") val setNumber: Int,
    @SerialName("weight_kg") val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    @SerialName("is_warmup") val isWarmup: Boolean,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("estimated_1rm_kg") val estimated1rmKg: Double?,
    @SerialName("completed_at") val completedAt: String?,
)

@Serializable
data class CompletionExerciseDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("order_index") val orderIndex: Int,
    val notes: String?,
    @SerialName("exercise_score") val exerciseScore: Double?,
    @SerialName("performance_grade") val performanceGrade: String?,
    val sets: List<CompletionSetDto>,
)

@Serializable
data class CompletionSessionDto(
    val id: String,
    @SerialName("template_id") val templateId: String?,
    val name: String,
    @SerialName("gate_difficulty") val gateDifficulty: String?,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("total_volume_kg") val totalVolumeKg: Double,
    @SerialName("completion_score") val completionScore: Double?,
    @SerialName("progress_score") val progressScore: Double?,
    @SerialName("quality_score") val qualityScore: Double?,
    @SerialName("gate_score") val gateScore: Double?,
    @SerialName("gate_clear_rank") val gateClearRank: String?,
    @SerialName("xp_earned") val xpEarned: Int?,
)

@Serializable
data class CompletionPayloadDto(
    val session: CompletionSessionDto,
    val exercises: List<CompletionExerciseDto>,
)

/** RPC param wrapper: complete_workout(payload jsonb). */
@Serializable
data class CompleteWorkoutParams(val payload: CompletionPayloadDto)

private fun CompletionSetPayload.toDto() = CompletionSetDto(
    setNumber, weightKg, reps, rpe, isWarmup, isCompleted, estimated1rmKg, completedAt,
)

private fun CompletionExercisePayload.toDto() = CompletionExerciseDto(
    exerciseId, orderIndex, notes, exerciseScore, performanceGrade, sets.map { it.toDto() },
)

private fun CompletionSession.toDto() = CompletionSessionDto(
    id, templateId, name, gateDifficulty, startedAt, completedAt, durationSeconds, totalVolumeKg,
    completionScore, progressScore, qualityScore, gateScore, gateClearRank, xpEarned,
)

fun CompletionPayload.toDto() = CompletionPayloadDto(session.toDto(), exercises.map { it.toDto() })

// ---- apply_session_progression ----

@Serializable
data class ProgressionPersistDto(
    val level: Int,
    @SerialName("current_xp") val currentXp: Int,
    @SerialName("lifetime_xp") val lifetimeXp: Int,
    @SerialName("strength_score") val strengthScore: Double,
    @SerialName("physique_score") val physiqueScore: Double,
    @SerialName("endurance_score") val enduranceScore: Double,
    @SerialName("discipline_score") val disciplineScore: Double,
    @SerialName("hunter_score") val hunterScore: Double,
    @SerialName("hunter_rank") val hunterRank: String,
    @SerialName("current_streak_days") val currentStreakDays: Int,
    @SerialName("longest_streak_days") val longestStreakDays: Int,
)

@Serializable
data class ApplyProgressionParams(
    @SerialName("p_session_id") val sessionId: String,
    val p: ProgressionPersistDto,
)

fun ProgressionPersistPayload.toDto() = ProgressionPersistDto(
    level, currentXp, lifetimeXp, strengthScore, physiqueScore, enduranceScore, disciplineScore,
    hunterScore, hunterRank.name, currentStreakDays, longestStreakDays,
)

// ---- apply_workout_results ----

@Serializable
data class PrRowDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("set_number") val setNumber: Int,
    @SerialName("record_type") val recordType: String,
    @SerialName("previous_value") val previousValue: Double?,
    @SerialName("new_value") val newValue: Double,
)

@Serializable
data class StatRowDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("best_weight_kg") val bestWeightKg: Double?,
    @SerialName("best_reps") val bestReps: Double?,
    @SerialName("best_estimated_1rm_kg") val bestEstimated1rmKg: Double?,
    @SerialName("best_volume_kg") val bestVolumeKg: Double?,
)

@Serializable
data class ApplyResultsParams(
    @SerialName("p_session_id") val sessionId: String,
    @SerialName("p_prs") val prs: List<PrRowDto>,
    @SerialName("p_stats") val stats: List<StatRowDto>,
)

fun DetectedPR.toDto() = PrRowDto(exerciseId, orderIndex, setNumber, recordType.wire, previousValue, newValue)

fun NewStat.toDto() = StatRowDto(exerciseId, bestWeightKg, bestReps, bestEstimated1rmKg, bestVolumeKg)

// ---- quest RPC params ----

@Serializable
data class SessionIdParam(@SerialName("p_session_id") val sessionId: String)

@Serializable
data class UserQuestIdParam(@SerialName("p_user_quest_id") val userQuestId: String)

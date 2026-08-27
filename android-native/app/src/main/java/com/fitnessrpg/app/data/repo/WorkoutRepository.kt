package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.CompleteWorkoutParams
import com.fitnessrpg.app.data.dto.toDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.workouts.CompletionPayload
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ExerciseGateBaseline(val averageVolumeKg: Double?, val recentBest1rmKg: Double?, val sessionCount: Int)

@Serializable
private data class ExercisePerformanceEvidenceDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("session_best_1rm_kg") val sessionBest1rmKg: Double,
    @SerialName("performed_at") val performedAt: String,
)

@Serializable
private data class ExerciseGateBaselineDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("recent_average_volume_kg") val averageVolumeKg: Double? = null,
    @SerialName("recent_best_1rm_kg") val recentBest1rmKg: Double? = null,
    @SerialName("session_count") val sessionCount: Int = 0,
)

@Serializable
private data class PriorSessionVolumeDto(@SerialName("total_volume_kg") val totalVolumeKg: Double? = null)

/** Persists a finished workout atomically via the complete_workout RPC. */
class WorkoutRepository {

    private val db get() = SupabaseProvider.client

    /**
     * Persist a finished workout atomically. Returns the session id. Idempotent:
     * retrying with the same session id returns the id without duplicating.
     */
    suspend fun completeWorkout(payload: CompletionPayload): String =
        db.postgrest.rpc("complete_workout", CompleteWorkoutParams(payload.toDto()).toJsonObject())
            .decodeAs<String>()

    suspend fun getExerciseGateBaselines(exerciseIds: List<String>): Map<String, ExerciseGateBaseline> {
        if (exerciseIds.isEmpty()) return emptyMap()
        return db.from("exercise_recent_baselines_v2").select {
            filter { isIn("exercise_id", exerciseIds.distinct()) }
        }.decodeList<ExerciseGateBaselineDto>().associate {
            it.exerciseId to ExerciseGateBaseline(it.averageVolumeKg, it.recentBest1rmKg, it.sessionCount)
        }
    }

    suspend fun getPriorTemplateVolume(templateId: String?): Double? {
        if (templateId == null) return null
        return db.from("workout_sessions").select {
            filter {
                eq("template_id", templateId)
                eq("status", "completed")
            }
            order("completed_at", Order.DESCENDING)
            limit(1)
        }.decodeList<PriorSessionVolumeDto>().firstOrNull()?.totalVolumeKg
    }

    /** Chronological per-session evidence; lifetime PRs are never treated as sessions. */
    suspend fun getExercisePerformanceHistory(exerciseIds: List<String>): Map<String, List<Double>> {
        if (exerciseIds.isEmpty()) return emptyMap()
        return db.from("exercise_performance_history_v3").select {
            filter { isIn("exercise_id", exerciseIds.distinct()) }
            order("performed_at", Order.ASCENDING)
        }.decodeList<ExercisePerformanceEvidenceDto>()
            .groupBy { it.exerciseId }
            .mapValues { (_, rows) -> rows.map { it.sessionBest1rmKg } }
    }
}

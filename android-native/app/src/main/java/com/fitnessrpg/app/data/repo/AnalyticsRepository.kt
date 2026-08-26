package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.BodyAssessmentDto
import com.fitnessrpg.app.data.dto.ExerciseNameDto
import com.fitnessrpg.app.data.dto.PersonalRecordDto
import com.fitnessrpg.app.data.dto.ProfileBasicsDto
import com.fitnessrpg.app.data.dto.StatBestDto
import com.fitnessrpg.app.data.dto.WorkoutSessionDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.domain.analytics.ExerciseStatInput
import com.fitnessrpg.app.domain.analytics.PlayerData
import com.fitnessrpg.app.domain.analytics.WeightPoint
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** One parallel fetch of everything the Player screen needs (all RLS-scoped). */
class AnalyticsRepository {

    private val db get() = SupabaseProvider.client

    suspend fun getPlayerData(userId: String): PlayerData = coroutineScope {
        val sessionsD = async {
            db.from("workout_sessions").select(
                Columns.list("id", "name", "completed_at", "gate_clear_rank", "total_volume_kg", "duration_seconds"),
            ) {
                filter { eq("status", "completed") }
                order("completed_at", Order.DESCENDING)
                limit(60)
            }.decodeList<WorkoutSessionDto>()
        }
        val statsD = async {
            db.from("exercise_user_stats").select(Columns.list("exercise_id", "best_estimated_1rm_kg"))
                .decodeList<StatBestDto>()
        }
        val profileD = async {
            db.from("profiles").select(Columns.list("sex", "current_weight_kg")) {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileBasicsDto>()
        }
        val prsD = async {
            db.from("personal_records").select(
                Columns.list("id", "exercise_id", "record_type", "new_value", "achieved_at"),
            ) {
                order("achieved_at", Order.DESCENDING)
                limit(20)
            }.decodeList<PersonalRecordDto>()
        }
        val weightsD = async {
            db.from("body_assessments").select(Columns.list("weight_kg", "assessment_date")) {
                order("assessment_date", Order.ASCENDING)
            }.decodeList<BodyAssessmentDto>()
        }

        val stats = statsD.await()
        val prs = prsD.await()

        // Only fetch the names for the exercises this user actually references —
        // NOT the whole ~1,300-row catalog (which made every Player load slow).
        val neededIds = (stats.map { it.exerciseId } + prs.map { it.exerciseId }).distinct()
        val nameById = if (neededIds.isEmpty()) {
            emptyMap()
        } else {
            db.from("exercises").select(Columns.list("id", "name")) {
                filter { isIn("id", neededIds) }
            }.decodeList<ExerciseNameDto>().associate { it.id to it.name }
        }

        val sessions = sessionsD.await()
        val profile = profileD.await()
        val weights = weightsD.await()

        PlayerData(
            sessions = sessions.map { it.toSummary() },
            stats = stats.map { ExerciseStatInput(it.exerciseId, nameById[it.exerciseId] ?: "", it.bestEstimated1rmKg) },
            bodyweightKg = profile?.currentWeightKg,
            sex = profile?.sex,
            prs = prs.map { it.toHistory(nameById[it.exerciseId] ?: "Exercise") },
            weights = weights.filter { it.weightKg != null && it.assessmentDate != null }
                .map { WeightPoint(it.assessmentDate!!, it.weightKg!!) },
        )
    }
}

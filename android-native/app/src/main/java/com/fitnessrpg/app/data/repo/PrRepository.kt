package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.ApplyResultsParams
import com.fitnessrpg.app.data.dto.ExerciseUserStatDto
import com.fitnessrpg.app.data.dto.toDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.pr.DetectedPR
import com.fitnessrpg.app.domain.pr.NewStat
import com.fitnessrpg.app.domain.pr.PriorStat
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

/** Fetches prior bests and persists detected PRs + updated stats via RPC. */
class PrRepository {

    private val db get() = SupabaseProvider.client

    /** Fetch the user's prior bests for the given exercises (RLS-scoped). */
    suspend fun getExerciseStats(exerciseIds: List<String>): Map<String, PriorStat> {
        if (exerciseIds.isEmpty()) return emptyMap()
        return db.from("exercise_user_stats").select {
            filter { isIn("exercise_id", exerciseIds) }
        }.decodeList<ExerciseUserStatDto>().associate { it.exerciseId to it.toPriorStat() }
    }

    /** Persist detected PRs + updated stats atomically/idempotently via the RPC. */
    suspend fun applyWorkoutResults(sessionId: String, prs: List<DetectedPR>, stats: List<NewStat>) {
        db.postgrest.rpc(
            "apply_workout_results",
            ApplyResultsParams(sessionId, prs.map { it.toDto() }, stats.map { it.toDto() }).toJsonObject(),
        )
    }
}

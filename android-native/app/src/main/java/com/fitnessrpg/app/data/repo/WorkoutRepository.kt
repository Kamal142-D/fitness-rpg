package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.CompleteWorkoutParams
import com.fitnessrpg.app.data.dto.toDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.workouts.CompletionPayload
import io.github.jan.supabase.postgrest.postgrest

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
}

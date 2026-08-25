package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.ApplyProgressionParams
import com.fitnessrpg.app.data.dto.ProgressionDto
import com.fitnessrpg.app.data.dto.toDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.progression.ProgressionPersistPayload
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

/** Reads progression and persists the post-workout progression snapshot. */
class ProgressionRepository {

    private val db get() = SupabaseProvider.client

    suspend fun getProgression(userId: String): PlayerProgression? =
        db.from("player_progression").select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<ProgressionDto>()?.toDomain()

    /** Persist a computed progression snapshot for a session (guarded, idempotent). */
    suspend fun applySessionProgression(sessionId: String, payload: ProgressionPersistPayload) {
        db.postgrest.rpc(
            "apply_session_progression",
            ApplyProgressionParams(sessionId, payload.toDto()).toJsonObject(),
        )
    }
}

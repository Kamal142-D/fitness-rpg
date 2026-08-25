package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.QuestDto
import com.fitnessrpg.app.data.dto.SessionIdParam
import com.fitnessrpg.app.data.dto.UserQuestDto
import com.fitnessrpg.app.data.dto.UserQuestIdParam
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.emptyRpcParams
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.quests.UserQuestView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

/** Daily/weekly quests: assignment, progress, and claiming (via RPCs). */
class QuestRepository {

    private val db get() = SupabaseProvider.client

    suspend fun ensureActiveQuests() {
        db.postgrest.rpc("ensure_active_quests", emptyRpcParams)
    }

    suspend fun recordWorkoutForQuests(sessionId: String) {
        db.postgrest.rpc("record_workout_for_quests", SessionIdParam(sessionId).toJsonObject())
    }

    suspend fun claimQuest(userQuestId: String) {
        db.postgrest.rpc("claim_quest", UserQuestIdParam(userQuestId).toJsonObject())
    }

    /** Ensure current quests exist, then list them joined with their definitions. */
    suspend fun listUserQuests(): List<UserQuestView> {
        ensureActiveQuests()

        val nowIso = Instant.now().toString()
        val userQuests = db.from("user_quests").select(
            Columns.list("id", "quest_id", "progress", "completed", "claimed", "expires_at", "assigned_at"),
        ) {
            filter { gt("expires_at", nowIso) }
            order("assigned_at", Order.DESCENDING)
        }.decodeList<UserQuestDto>()
        if (userQuests.isEmpty()) return emptyList()

        val questIds = userQuests.map { it.questId }.distinct()
        val quests = db.from("quests").select(
            Columns.list("id", "name", "description", "type", "requirement_value", "xp_reward"),
        ) {
            filter { isIn("id", questIds) }
        }.decodeList<QuestDto>()
        val byId = quests.associateBy { it.id }

        return userQuests.filter { byId.containsKey(it.questId) }.map { uq ->
            val def = byId.getValue(uq.questId)
            UserQuestView(
                id = uq.id,
                questId = uq.questId,
                name = def.name,
                description = def.description,
                type = def.type,
                requirementValue = def.requirementValue,
                progress = uq.progress,
                completed = uq.completed,
                claimed = uq.claimed,
                xpReward = def.xpReward,
                expiresAt = uq.expiresAt,
            )
        }
    }
}

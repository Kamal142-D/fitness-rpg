package com.fitnessrpg.app.domain.quests

import kotlinx.serialization.Serializable

/** A user's active quest joined with its definition (for the Quests screen). */
@Serializable
data class UserQuestView(
    /** user_quests row id (target of claim). */
    val id: String,
    val questId: String,
    val name: String,
    val description: String?,
    val type: String, // 'daily' | 'weekly' | ...
    val requirementValue: Double,
    val progress: Double,
    val completed: Boolean,
    val claimed: Boolean,
    val xpReward: Int,
    val expiresAt: String?,
)

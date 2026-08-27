package com.fitnessrpg.app.domain.model

import com.fitnessrpg.app.domain.rank.Rank
import kotlinx.serialization.Serializable

/** The player's durable progression + attributes (mirrors `player_progression`). */
@Serializable
data class PlayerProgression(
    val level: Int,
    val currentXp: Int,
    val lifetimeXp: Int,
    val strengthScore: Double,
    val physiqueScore: Double,
    val enduranceScore: Double,
    val conditioningScore: Double? = null,
    val disciplineScore: Double,
    val hunterScore: Double,
    val hunterRank: Rank,
    val hunterRp: Int = 0,
    val physiqueRp: Int = 0,
    val strengthRp: Int = 0,
    val conditioningRp: Int = 0,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val hunterRankProvisional: Boolean = true,
    val hunterRankConfidence: String = "low",
    val hunterRankCap: Rank? = Rank.C,
    val hunterRankReasons: List<String> = emptyList(),
    val assessmentUpdateRequired: Boolean = false,
)

/** The user's profile (mirrors `profiles`). Nullable fields are unset pre-onboarding. */
@Serializable
data class Profile(
    val id: String,
    val displayName: String?,
    val dateOfBirth: String?,
    val sex: String?,
    val heightCm: Double?,
    val currentWeightKg: Double?,
    val experienceLevel: String?,
    val fitnessGoal: String?,
    val trainingDaysPerWeek: Int?,
    val trainingLocation: String?,
    val preferredWorkoutMinutes: Int?,
    val onboardingCompleted: Boolean,
)

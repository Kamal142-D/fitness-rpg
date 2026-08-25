package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.auth.friendlyAuthError
import com.fitnessrpg.app.data.dto.BodyAssessmentInsertDto
import com.fitnessrpg.app.data.dto.ProfileDto
import com.fitnessrpg.app.data.dto.ProfileOnboardingUpdateDto
import com.fitnessrpg.app.data.dto.ProgressionInitUpdateDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.domain.onboarding.OnboardingDraft
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import io.github.jan.supabase.postgrest.from

sealed interface OnboardingOutcome {
    data object Ok : OnboardingOutcome
    data class Error(val message: String) : OnboardingOutcome
}

/** Reads the profile and persists the completed Awakening. */
class ProfileRepository {

    private val db get() = SupabaseProvider.client

    suspend fun getProfile(userId: String): Profile? =
        db.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<ProfileDto>()?.toDomain()

    /**
     * Persist a completed Awakening: profile fields + onboarding flag, an optional
     * body-composition assessment, and the initial progression attributes. The
     * profile and progression rows already exist (created by handle_new_user).
     */
    suspend fun completeOnboarding(
        userId: String,
        draft: OnboardingDraft,
        hunter: HunterRankResult,
    ): OnboardingOutcome = try {
        val profileUpdate = ProfileOnboardingUpdateDto(
            displayName = draft.displayName.trim(),
            dateOfBirth = draft.dateOfBirth,
            sex = draft.sex?.wire,
            heightCm = draft.heightCm,
            currentWeightKg = draft.currentWeightKg,
            experienceLevel = draft.experienceLevel?.wire,
            fitnessGoal = draft.fitnessGoal?.wire,
            trainingDaysPerWeek = draft.trainingDaysPerWeek,
            trainingLocation = draft.trainingLocation?.wire,
            preferredWorkoutMinutes = draft.preferredWorkoutMinutes,
            onboardingCompleted = true,
        )
        db.from("profiles").update(profileUpdate) { filter { eq("id", userId) } }

        // Store only body fat + weight. We deliberately do NOT write the user's
        // (total) "Muscle Mass" into the skeletal_muscle_mass_kg column — they are
        // different measurements. SMM stays null unless a true SMM value is known.
        if (draft.bodyFatPercent != null) {
            db.from("body_assessments").insert(
                BodyAssessmentInsertDto(
                    userId = userId,
                    weightKg = draft.currentWeightKg,
                    bodyFatPercent = draft.bodyFatPercent,
                    skeletalMuscleMassKg = null,
                    source = "manual",
                ),
            )
        }

        db.from("player_progression").update(
            ProgressionInitUpdateDto(
                strengthScore = hunter.strengthScore ?: 0.0,
                physiqueScore = hunter.physiqueScore ?: 0.0,
                enduranceScore = 0.0,
                disciplineScore = 0.0,
                hunterScore = hunter.hunterScore,
                hunterRank = hunter.rank.name,
            ),
        ) { filter { eq("user_id", userId) } }

        OnboardingOutcome.Ok
    } catch (e: Exception) {
        OnboardingOutcome.Error(friendlyAuthError(e.message))
    }
}

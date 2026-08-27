package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.cache.DataCache
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.data.dto.BodyAssessmentInsertDto
import com.fitnessrpg.app.data.dto.ConditioningAssessmentInsertDto
import com.fitnessrpg.app.data.dto.StrengthAssessmentSetInsertDto
import com.fitnessrpg.app.data.dto.ProfileDto
import com.fitnessrpg.app.data.dto.ProfileOnboardingUpdateDto
import com.fitnessrpg.app.data.dto.ProfilePhysicalUpdateDto
import com.fitnessrpg.app.data.dto.ProgressionInitUpdateDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.domain.onboarding.OnboardingDraft
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.domain.rankings.BodyCompositionData
import com.fitnessrpg.app.domain.rankings.ConditioningInput
import com.fitnessrpg.app.domain.rankings.StrengthAssessmentInput
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
        if (draft.bodyFatPercent != null || draft.waistCm != null || draft.skeletalMuscleMassKg != null) {
            db.from("body_assessments").insert(
                BodyAssessmentInsertDto(
                    userId = userId,
                    weightKg = draft.currentWeightKg,
                    bodyFatPercent = draft.bodyFatPercent,
                    skeletalMuscleMassKg = draft.skeletalMuscleMassKg,
                    waistCm = draft.waistCm,
                    source = draft.bodyAssessmentSource,
                ),
            )
        }

        if (draft.conditioningTestType != null && draft.conditioningResult != null) {
            db.from("conditioning_assessments").insert(
                ConditioningAssessmentInsertDto(userId, draft.conditioningTestType, draft.conditioningResult, hunter.conditioningScore),
            )
        }
        if (draft.strengthAssessmentSets.isNotEmpty()) {
            db.from("strength_assessment_sets").insert(draft.strengthAssessmentSets.map { set ->
                StrengthAssessmentSetInsertDto(userId, set.exerciseId, set.variation, set.equipment.name.lowercase(), set.weightKg, set.reps, set.dumbbellWeightMode?.let { if (it.name == "PER_HAND") "per_hand" else "total" }, set.rpe)
            })
        }

        db.from("player_progression").update(
            ProgressionInitUpdateDto(
                strengthScore = hunter.strengthScore ?: 0.0,
                physiqueScore = hunter.physiqueScore ?: 0.0,
                enduranceScore = 0.0,
                conditioningScore = hunter.conditioningScore,
                disciplineScore = 0.0,
                hunterScore = hunter.hunterScore ?: 0.0,
                hunterRank = hunter.rank.wire,
                hunterRp = hunter.rp,
                physiqueRp = hunter.physique?.rp ?: 0,
                strengthRp = hunter.strength?.rp ?: 0,
                conditioningRp = hunter.conditioning?.rp ?: 0,
                hunterRankProvisional = hunter.provisional,
                hunterRankConfidence = hunter.confidence.name.lowercase(),
                hunterRankCap = hunter.rankCap?.wire,
                hunterRankReasons = hunter.reasons,
                assessmentUpdateRequired = hunter.provisional,
            ),
        ) { filter { eq("user_id", userId) } }

        OnboardingOutcome.Ok
    } catch (e: Exception) {
        OnboardingOutcome.Error(friendlyDataError(e, "Couldn't save your assessment. Please try again."))
    }

    suspend fun completeAssessment(
        userId: String,
        body: BodyCompositionData?,
        strength: List<StrengthAssessmentInput>,
        conditioning: ConditioningInput?,
    ): OnboardingOutcome = try {
        if (body != null) {
            db.from("profiles").update(ProfilePhysicalUpdateDto(body.heightCm, body.weightKg)) { filter { eq("id", userId) } }
            if (body.bodyFatPercent != null || body.waistCm != null || body.skeletalMuscleMassKg != null || body.leanBodyMassKg != null) {
                db.from("body_assessments").insert(
                    BodyAssessmentInsertDto(
                        userId = userId,
                        weightKg = body.weightKg,
                        bodyFatPercent = body.bodyFatPercent,
                        skeletalMuscleMassKg = body.skeletalMuscleMassKg,
                        waistCm = body.waistCm,
                        leanBodyMassKg = body.leanBodyMassKg,
                        leftArmLeanMassKg = body.segmentalLeanMass?.leftArmKg,
                        rightArmLeanMassKg = body.segmentalLeanMass?.rightArmKg,
                        leftLegLeanMassKg = body.segmentalLeanMass?.leftLegKg,
                        rightLegLeanMassKg = body.segmentalLeanMass?.rightLegKg,
                        source = body.source?.name?.lowercase() ?: "manual",
                    ),
                )
            }
        }
        if (strength.isNotEmpty()) {
            db.from("strength_assessment_sets").insert(strength.map { set ->
                StrengthAssessmentSetInsertDto(
                    userId, set.exerciseId, set.variation, set.equipment.name.lowercase(), set.weightKg,
                    set.reps, set.dumbbellWeightMode?.let { if (it == com.fitnessrpg.app.domain.rankings.DumbbellWeightMode.PER_HAND) "per_hand" else "total" }, set.rpe,
                )
            })
        }
        if (conditioning?.testType != null && conditioning.result != null) {
            db.from("conditioning_assessments").insert(
                ConditioningAssessmentInsertDto(userId, conditioning.testType.name.lowercase(), conditioning.result, null),
            )
        }
        AssessmentRepository().recalculateAndPersist(userId)
        // The rank, banner state and player data all just changed — drop the
        // cached screens so returning to Player/System shows the saved values.
        DataCache.invalidatePrefix("player:")
        DataCache.invalidatePrefix("system:")
        OnboardingOutcome.Ok
    } catch (e: Exception) {
        OnboardingOutcome.Error(friendlyDataError(e, "Couldn't save your assessment. Please try again."))
    }
}

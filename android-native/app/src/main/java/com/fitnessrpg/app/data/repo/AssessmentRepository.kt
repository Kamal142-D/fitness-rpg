package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.BodyAssessmentDto
import com.fitnessrpg.app.data.dto.ConditioningAssessmentDto
import com.fitnessrpg.app.data.dto.ProfileBasicsDto
import com.fitnessrpg.app.data.dto.ProgressionInitUpdateDto
import com.fitnessrpg.app.data.dto.StrengthAssessmentSetDto
import com.fitnessrpg.app.data.dto.StrengthEvidenceDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.domain.onboarding.ageFromDob
import com.fitnessrpg.app.domain.rankings.AssessmentConfidence
import com.fitnessrpg.app.domain.rankings.BodyAssessmentSource
import com.fitnessrpg.app.domain.rankings.BodyCompositionData
import com.fitnessrpg.app.domain.rankings.ConditioningInput
import com.fitnessrpg.app.domain.rankings.ConditioningRankResult
import com.fitnessrpg.app.domain.rankings.ConditioningTestType
import com.fitnessrpg.app.domain.rankings.DumbbellWeightMode
import com.fitnessrpg.app.domain.rankings.Equipment
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.domain.rankings.PhysiqueRankResult
import com.fitnessrpg.app.domain.rankings.SegmentalLeanMassData
import com.fitnessrpg.app.domain.rankings.StrengthAssessmentInput
import com.fitnessrpg.app.domain.rankings.StrengthRankResult
import com.fitnessrpg.app.domain.rankings.computeConditioningRank
import com.fitnessrpg.app.domain.rankings.computeHunterRank
import com.fitnessrpg.app.domain.rankings.computePhysiqueRank
import com.fitnessrpg.app.domain.rankings.computeStrengthRank
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Serializable
data class RankAssessmentSnapshot(
    val hunter: HunterRankResult,
    val physique: PhysiqueRankResult,
    val strength: StrengthRankResult,
    val conditioning: ConditioningRankResult,
    val profile: ProfileBasicsDto?,
    val latestBody: BodyAssessmentDto?,
    val latestConditioning: ConditioningAssessmentDto? = null,
    /**
     * True only when the user genuinely has something to add or refresh: no body
     * assessment on record (or it has gone stale), or no strength evidence yet.
     * Deliberately NOT tied to [HunterRankResult.provisional] — a rank stays
     * provisional while conditioning is skipped, but conditioning is optional, so
     * a completed body + strength pass must be able to clear the nag banner.
     */
    val needsAssessmentUpdate: Boolean = false,
)

class AssessmentRepository {
    private val db get() = SupabaseProvider.client

    suspend fun getRankAssessment(userId: String): RankAssessmentSnapshot {
        val profile = db.from("profiles").select(Columns.list("sex", "date_of_birth", "current_weight_kg", "height_cm")) {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<ProfileBasicsDto>()
        val body = db.from("body_assessments").select {
            filter { eq("user_id", userId) }
            order("assessment_date", Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<BodyAssessmentDto>()
        val manualStrength = db.from("strength_assessment_sets").select {
            filter { eq("user_id", userId) }
            order("assessed_at", Order.DESCENDING)
            limit(100)
        }.decodeList<StrengthAssessmentSetDto>()
        val workoutStrength = db.from("strength_evidence_v2").select {
            filter { eq("user_id", userId) }
            order("performed_at", Order.DESCENDING)
            limit(300)
        }.decodeList<StrengthEvidenceDto>()
        val conditioningDto = db.from("conditioning_assessments").select {
            filter { eq("user_id", userId) }
            order("assessed_at", Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<ConditioningAssessmentDto>()

        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val age = profile?.dateOfBirth?.let { runCatching { ageFromDob(it) }.getOrNull() }
        val physique = computePhysiqueRank(
            BodyCompositionData(
                weightKg = body?.weightKg ?: profile?.currentWeightKg ?: 0.0,
                heightCm = profile?.heightCm ?: 0.0,
                bodyFatPercent = body?.bodyFatPercent,
                muscleMassKg = body?.muscleMassKg,
                skeletalMuscleMassKg = body?.skeletalMuscleMassKg,
                leanBodyMassKg = body?.leanBodyMassKg,
                waistCm = body?.waistCm,
                ageYears = age,
                source = body?.source.toBodySource(),
                assessedAtEpochDay = body?.assessmentDate?.toEpochDayOrNull(),
                segmentalLeanMass = SegmentalLeanMassData(body?.leftArmLeanMassKg, body?.rightArmLeanMassKg, body?.leftLegLeanMassKg, body?.rightLegLeanMassKg),
                sex = profile?.sex,
            ),
            today,
        )
        val strengthInputs = buildList {
            manualStrength.forEach { row ->
                add(StrengthAssessmentInput(row.exerciseId, row.equipment.toEquipment(), row.weightKg, row.reps, row.weightMode.toWeightMode(), row.variation, row.rpe, row.assessedAt.toEpochDayOrNull(), "assessment:${row.assessedAt}"))
            }
            workoutStrength.filter { it.equipment.lowercase() != "dumbbell" }.forEach { row ->
                add(StrengthAssessmentInput(row.exerciseName, row.equipment.toEquipment(), row.weightKg, row.reps, null, row.variation, row.rpe, row.performedAt.toEpochDayOrNull(), row.sessionId))
            }
        }
        val recentStrengthInputs = strengthInputs.filter { day ->
            day.performedAtEpochDay?.let { today - it <= com.fitnessrpg.app.domain.rankings.RankingV3Config.STRENGTH_VALID_DAYS } == true
        }
        val rankingStrengthInputs = recentStrengthInputs.ifEmpty { strengthInputs }
        val sessions = rankingStrengthInputs.mapNotNull { it.sessionId }.distinct().size
        val strength = computeStrengthRank(rankingStrengthInputs, profile?.currentWeightKg ?: body?.weightKg ?: 0.0, profile?.sex, sessions, today, age)
        val conditioning = conditioningDto?.let {
            val type = runCatching { ConditioningTestType.valueOf(it.testType.uppercase()) }.getOrNull()
            computeConditioningRank(ConditioningInput(type, it.result, age, profile?.sex, it.assessedAt.toEpochDayOrNull()), today)
        } ?: ConditioningRankResult(null, null, true, AssessmentConfidence.LOW, listOf("A standardized conditioning test has not been completed."))
        val hunter = computeHunterRank(physique, strength, conditioning)
        // The nag is about missing/stale data the user can actually supply — a
        // fresh body assessment plus some strength evidence. Conditioning is
        // optional and never keeps this flag on by itself.
        val hasStrengthEvidence = rankingStrengthInputs.isNotEmpty()
        val bodyFresh = body != null && !physique.stale
        val needsAssessmentUpdate = !bodyFresh || !hasStrengthEvidence
        return RankAssessmentSnapshot(
            hunter = hunter,
            physique = physique,
            strength = strength,
            conditioning = conditioning,
            profile = profile,
            latestBody = body,
            latestConditioning = conditioningDto,
            needsAssessmentUpdate = needsAssessmentUpdate,
        )
    }

    suspend fun recalculateAndPersist(userId: String): RankAssessmentSnapshot {
        val snapshot = getRankAssessment(userId)
        val result = snapshot.hunter
        db.from("player_progression").update(
            ProgressionInitUpdateDto(
                strengthScore = result.strengthScore ?: 0.0,
                physiqueScore = result.physiqueScore ?: 0.0,
                enduranceScore = 0.0,
                conditioningScore = result.conditioningScore,
                disciplineScore = 0.0,
                hunterScore = result.hunterScore ?: 0.0,
                hunterRank = result.rank.wire,
                hunterRp = result.rp,
                physiqueRp = result.physique?.rp ?: 0,
                strengthRp = result.strength?.rp ?: 0,
                conditioningRp = result.conditioning?.rp ?: 0,
                hunterRankProvisional = result.provisional,
                hunterRankConfidence = result.confidence.name.lowercase(),
                hunterRankCap = result.rankCap?.wire,
                hunterRankReasons = result.reasons,
                assessmentUpdateRequired = snapshot.needsAssessmentUpdate,
            ),
        ) { filter { eq("user_id", userId) } }
        return snapshot
    }
}

private fun String?.toBodySource(): BodyAssessmentSource? = when (this) {
    "inbody" -> BodyAssessmentSource.INBODY
    "smart_scale" -> BodyAssessmentSource.SMART_SCALE
    "manual" -> BodyAssessmentSource.MANUAL
    "other" -> BodyAssessmentSource.OTHER
    else -> null
}

private fun String.toEquipment(): Equipment = when (lowercase().replace(' ', '_')) {
    "barbell" -> Equipment.BARBELL
    "dumbbell" -> Equipment.DUMBBELL
    "machine" -> Equipment.MACHINE
    "smith_machine", "smith" -> Equipment.SMITH_MACHINE
    "cable" -> Equipment.CABLE
    "bodyweight", "body_weight" -> Equipment.BODYWEIGHT
    else -> Equipment.OTHER
}

private fun String?.toWeightMode(): DumbbellWeightMode? = when (this) {
    "per_hand" -> DumbbellWeightMode.PER_HAND
    "total", "combined" -> DumbbellWeightMode.COMBINED
    else -> null
}

private fun String.toEpochDayOrNull(): Long? = runCatching {
    if (length == 10) LocalDate.parse(this).toEpochDay()
    else Instant.parse(this).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
}.getOrNull()

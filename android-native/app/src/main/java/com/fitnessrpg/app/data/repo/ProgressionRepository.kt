package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.ApplyProgressionParams
import com.fitnessrpg.app.data.dto.BodyAssessmentDto
import com.fitnessrpg.app.data.dto.ExerciseNameDto
import com.fitnessrpg.app.data.dto.ProfileBasicsDto
import com.fitnessrpg.app.data.dto.ProgressionDto
import com.fitnessrpg.app.data.dto.StatBestDto
import com.fitnessrpg.app.data.dto.toDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.progression.FinishExercise
import com.fitnessrpg.app.domain.progression.FinishInputs
import com.fitnessrpg.app.domain.progression.ProgressionPersistPayload
import com.fitnessrpg.app.domain.ranking.PhysiqueInput
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

    /** Gather the inputs needed to recompute attributes after a workout. */
    suspend fun getFinishInputs(userId: String): FinishInputs = coroutineScope {
        val profileD = async {
            db.from("profiles").select(Columns.list("sex", "current_weight_kg", "height_cm")) {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<ProfileBasicsDto>()
        }
        val statsD = async {
            db.from("exercise_user_stats").select(Columns.list("exercise_id", "best_estimated_1rm_kg"))
                .decodeList<StatBestDto>()
        }
        val exercisesD = async {
            db.from("exercises").select(Columns.list("id", "name")).decodeList<ExerciseNameDto>()
        }
        val assessmentD = async {
            db.from("body_assessments").select(
                Columns.list("body_fat_percent", "skeletal_muscle_mass_kg", "weight_kg", "assessment_date"),
            ) {
                order("assessment_date", Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<BodyAssessmentDto>()
        }

        val profile = profileD.await()
        val stats = statsD.await()
        val exercises = exercisesD.await()
        val assessment = assessmentD.await()

        val sex = profile?.sex
        val nameById = exercises.associate { it.id to it.name }
        val finishExercises = stats.map { FinishExercise(nameById[it.exerciseId] ?: "", it.bestEstimated1rmKg) }
        val physique = assessment?.let {
            PhysiqueInput(it.bodyFatPercent, it.skeletalMuscleMassKg, it.weightKg, sex)
        }

        FinishInputs(
            bodyweightKg = profile?.currentWeightKg,
            heightCm = profile?.heightCm,
            sex = sex,
            exercises = finishExercises,
            assessment = physique,
        )
    }
}

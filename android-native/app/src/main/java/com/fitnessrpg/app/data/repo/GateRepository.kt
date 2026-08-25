package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.ExerciseDto
import com.fitnessrpg.app.data.dto.IdDto
import com.fitnessrpg.app.data.dto.TemplateExerciseDto
import com.fitnessrpg.app.data.dto.TemplateExerciseInsertDto
import com.fitnessrpg.app.data.dto.WorkoutTemplateDto
import com.fitnessrpg.app.data.dto.WorkoutTemplateInsertDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.domain.model.CreateGateInput
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.TemplateExerciseWithExercise
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/** Reads/writes for Gates (workout templates), their exercises, and the catalog. */
class GateRepository {

    private val db get() = SupabaseProvider.client

    /** All Gates visible to the user (system + own). RLS filters. */
    suspend fun listGates(): List<GateTemplate> =
        db.from("workout_templates").select {
            order("is_system_template", Order.DESCENDING)
            order("name", Order.ASCENDING)
        }.decodeList<WorkoutTemplateDto>().map { it.toDomain() }

    /** A Gate plus its ordered exercises (joined in Kotlin to stay fully typed). */
    suspend fun getGate(templateId: String): GateDetail? {
        val template = db.from("workout_templates").select {
            filter { eq("id", templateId) }
        }.decodeSingleOrNull<WorkoutTemplateDto>() ?: return null

        val tes = db.from("workout_template_exercises").select {
            filter { eq("template_id", templateId) }
            order("order_index", Order.ASCENDING)
        }.decodeList<TemplateExerciseDto>()

        val exerciseIds = tes.map { it.exerciseId }
        val exercises = if (exerciseIds.isNotEmpty()) {
            db.from("exercises").select {
                filter { isIn("id", exerciseIds) }
            }.decodeList<ExerciseDto>()
        } else {
            emptyList()
        }
        val byId = exercises.associateBy { it.id }

        return GateDetail(
            template = template.toDomain(),
            exercises = tes.filter { byId.containsKey(it.exerciseId) }.map { te ->
                TemplateExerciseWithExercise(te.toDomain(), byId.getValue(te.exerciseId).toDomain())
            },
        )
    }

    /** "Today's Gate": the Full Body starter, else the first system Gate. */
    suspend fun getRecommendedGate(): GateTemplate? {
        db.from("workout_templates").select {
            filter { eq("id", FULL_BODY_TEMPLATE_ID) }
        }.decodeSingleOrNull<WorkoutTemplateDto>()?.let { return it.toDomain() }

        return db.from("workout_templates").select {
            filter { eq("is_system_template", true) }
            order("name", Order.ASCENDING)
            limit(1)
        }.decodeList<WorkoutTemplateDto>().firstOrNull()?.toDomain()
    }

    /** The exercise catalog (for building custom Gates). */
    suspend fun listExercises(): List<Exercise> =
        db.from("exercises").select {
            order("category", Order.ASCENDING)
            order("name", Order.ASCENDING)
        }.decodeList<ExerciseDto>().map { it.toDomain() }

    /** Create a custom Gate owned by the user, with default targets per exercise. */
    suspend fun createGate(userId: String, input: CreateGateInput): String {
        val duration = minOf(360, maxOf(10, input.exerciseIds.size * 9))
        val templateRow = WorkoutTemplateInsertDto(
            userId = userId,
            name = input.name.trim(),
            difficulty = input.difficulty.name,
            isSystemTemplate = false,
            estimatedDurationMinutes = duration,
            description = "Custom gate",
        )
        val inserted = db.from("workout_templates").insert(templateRow) {
            select(Columns.list("id"))
        }.decodeSingle<IdDto>()

        val rows = input.exerciseIds.mapIndexed { i, exerciseId ->
            TemplateExerciseInsertDto(
                templateId = inserted.id,
                exerciseId = exerciseId,
                orderIndex = i,
                targetSets = 3,
                targetRepsMin = 8,
                targetRepsMax = 12,
                targetRpe = 8,
                restSeconds = 90,
            )
        }
        db.from("workout_template_exercises").insert(rows)
        return inserted.id
    }

    companion object {
        private const val FULL_BODY_TEMPLATE_ID = "10000000-0000-0000-0000-000000000006"
    }
}

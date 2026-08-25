package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.ExerciseDto
import com.fitnessrpg.app.data.dto.IdDto
import com.fitnessrpg.app.data.dto.TemplateExerciseDto
import com.fitnessrpg.app.data.dto.TemplateExerciseInsertDto
import com.fitnessrpg.app.data.dto.WorkoutTemplateDto
import com.fitnessrpg.app.data.dto.WorkoutTemplateInsertDto
import com.fitnessrpg.app.data.dto.WorkoutTemplateNameUpdateDto
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.model.CreateGateInput
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.TemplateExerciseWithExercise
import com.fitnessrpg.app.domain.gates.activeGateTemplates
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Reads/writes for Gates (workout templates), their exercises, and the catalog. */
class GateRepository {

    private val db get() = SupabaseProvider.client

    /** All Gates visible to the user (system + own). RLS filters. */
    suspend fun listGates(): List<GateTemplate> {
        val hidden = db.from("hidden_system_templates").select().decodeList<HiddenTemplateDto>().map { it.templateId }.toSet()
        return db.from("workout_templates").select {
            order("is_system_template", Order.DESCENDING)
            order("name", Order.ASCENDING)
        }.decodeList<WorkoutTemplateDto>().map { it.toDomain() }.let { activeGateTemplates(it, hidden) }
    }

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
    suspend fun listExercises(): List<Exercise> {
        val all = mutableListOf<ExerciseDto>()
        var offset = 0L
        do {
            val page = db.from("exercises").select {
                order("category", Order.ASCENDING)
                order("name", Order.ASCENDING)
                range(offset, offset + EXERCISE_PAGE_SIZE - 1)
            }.decodeList<ExerciseDto>()
            all += page
            offset += page.size
        } while (page.size == EXERCISE_PAGE_SIZE.toInt())
        return all.distinctBy { it.id }.map { it.toDomain() }
    }

    suspend fun getExercises(ids: List<String>): Map<String, Exercise> = if (ids.isEmpty()) emptyMap() else
        db.from("exercises").select { filter { isIn("id", ids.distinct()) } }
            .decodeList<ExerciseDto>().associate { it.id to it.toDomain() }

    /** Create a custom Gate owned by the user, with default targets per exercise. */
    suspend fun createGate(userId: String, input: CreateGateInput): String {
        val duration = minOf(360, maxOf(10, input.exerciseIds.size * 9))
        val templateRow = WorkoutTemplateInsertDto(
            userId = userId,
            name = input.name.trim(),
            difficulty = null,
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

    suspend fun archiveGate(templateId: String) {
        db.postgrest.rpc("archive_workout_template", ArchiveTemplateParams(templateId).toJsonObject())
    }

    suspend fun updateGate(templateId: String, input: CreateGateInput) {
        val duration = minOf(360, maxOf(10, input.exerciseIds.size * 9))
        db.from("workout_templates").update(WorkoutTemplateNameUpdateDto(input.name.trim(), duration)) {
            filter { eq("id", templateId) }
        }
        db.from("workout_template_exercises").delete { filter { eq("template_id", templateId) } }
        val rows = input.exerciseIds.mapIndexed { i, exerciseId ->
            TemplateExerciseInsertDto(templateId, exerciseId, i, 3, 8, 12, 8, 90)
        }
        if (rows.isNotEmpty()) db.from("workout_template_exercises").insert(rows)
    }

    suspend fun hideSystemGate(userId: String, templateId: String) {
        db.from("hidden_system_templates").upsert(HiddenTemplateInsertDto(userId, templateId))
    }

    suspend fun duplicateGate(userId: String, templateId: String): String {
        val detail = getGate(templateId) ?: error("Gate not found")
        return createGate(userId, CreateGateInput("${detail.template.name} Copy", detail.exercises.map { it.exercise.id }))
    }

    companion object {
        private const val FULL_BODY_TEMPLATE_ID = "10000000-0000-0000-0000-000000000006"
        private const val EXERCISE_PAGE_SIZE = 1000L
    }
}

@Serializable private data class HiddenTemplateDto(@SerialName("template_id") val templateId: String)
@Serializable private data class HiddenTemplateInsertDto(@SerialName("user_id") val userId: String, @SerialName("template_id") val templateId: String)
@Serializable private data class ArchiveTemplateParams(@SerialName("p_template_id") val templateId: String)

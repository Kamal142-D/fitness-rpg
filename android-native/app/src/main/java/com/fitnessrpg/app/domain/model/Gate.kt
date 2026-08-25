package com.fitnessrpg.app.domain.model

/** Exercise catalog entry (mirrors the `exercises` table). */
data class Exercise(
    val id: String,
    val name: String,
    val category: String?,
    val primaryMuscleGroup: String?,
    val secondaryMuscleGroups: List<String> = emptyList(),
    val equipment: String?,
    val exerciseType: String?,
    val rankingEnabled: Boolean,
    val createdAt: String? = null,
)

/** A Gate template (mirrors `workout_templates`). `difficulty` is raw text. */
data class GateTemplate(
    val id: String,
    val userId: String?,
    val name: String,
    val description: String?,
    val estimatedDurationMinutes: Int?,
    val difficulty: String?,
    val isSystemTemplate: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** A template's exercise slot with targets (mirrors `workout_template_exercises`). */
data class TemplateExercise(
    val id: String,
    val templateId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetRpe: Double?,
    val restSeconds: Int?,
)

/** A template exercise joined with its catalog entry. */
data class TemplateExerciseWithExercise(
    val templateExercise: TemplateExercise,
    val exercise: Exercise,
)

/** A template joined with its ordered exercises + catalog info. */
data class GateDetail(
    val template: GateTemplate,
    val exercises: List<TemplateExerciseWithExercise>,
)

/** Input for creating a custom Gate. */
data class CreateGateInput(
    val name: String,
    val difficulty: com.fitnessrpg.app.domain.rank.Rank,
    val exerciseIds: List<String>,
)

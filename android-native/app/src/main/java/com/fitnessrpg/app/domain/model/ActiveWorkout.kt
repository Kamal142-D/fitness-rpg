package com.fitnessrpg.app.domain.model

import com.fitnessrpg.app.domain.rank.Rank

/** A single set within the active workout. */
data class ActiveSet(
    val id: String,
    val setNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val completedAt: String?,
)

/** An exercise within the active workout. */
data class ActiveExercise(
    val id: String,
    val exerciseId: String,
    val name: String,
    val primaryMuscle: String?,
    val targetSets: Int?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetRpe: Double?,
    val restSeconds: Int?,
    val rankingEnabled: Boolean,
    val notes: String,
    val sets: List<ActiveSet>,
)

/** The in-progress workout. Persisted as a draft so it survives app restarts. */
data class ActiveWorkout(
    /** Client-generated session id (idempotency key for completion). */
    val sessionId: String,
    val templateId: String?,
    val name: String,
    val gateDifficulty: Rank?,
    val startedAt: String,
    val currentExerciseIndex: Int,
    /** Epoch ms when the current rest ends, or null when not resting. */
    val restEndsAt: Long?,
    val exercises: List<ActiveExercise>,
)

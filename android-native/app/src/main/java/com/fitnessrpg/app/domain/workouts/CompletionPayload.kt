package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.model.ActiveWorkout
import com.fitnessrpg.app.util.isoFromMillis
import com.fitnessrpg.app.util.millisFromIso
import com.fitnessrpg.app.util.round2

/**
 * Transform the active workout into the atomic `complete_workout` RPC payload,
 * plus summary aggregates for the completion screen. Pure and testable.
 *
 * Only completed sets are persisted, and only exercises with at least one
 * completed set. Set numbers and exercise order are re-sequenced.
 */
data class CompletionSetPayload(
    val setNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Double?,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val estimated1rmKg: Double?,
    val completedAt: String?,
)

data class CompletionExercisePayload(
    val exerciseId: String,
    val orderIndex: Int,
    val notes: String?,
    /** Filled by the ranking step (gateResult) before completion; null otherwise. */
    val exerciseScore: Double?,
    val performanceGrade: String?,
    val sets: List<CompletionSetPayload>,
    val difficultyScore: Double? = null,
    val difficultyRank: String? = null,
)

data class CompletionSession(
    val id: String,
    val templateId: String?,
    val name: String,
    val gateDifficulty: String?,
    val startedAt: String,
    val completedAt: String,
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val completionScore: Double?,
    val progressScore: Double?,
    val qualityScore: Double?,
    val gateScore: Double?,
    val gateClearRank: String?,
    val xpEarned: Int?,
    val gateDifficultyScore: Double? = null,
    val gateDifficultyRank: String? = null,
)

data class CompletionPayload(
    val session: CompletionSession,
    val exercises: List<CompletionExercisePayload>,
)

data class CompletionAggregates(
    val name: String,
    val gateDifficulty: String?,
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val completedSets: Int,
    /** Non-warm-up sets that were planned (completed or not) — for completion %. */
    val plannedWorkingSets: Int,
    val exerciseCount: Int,
)

data class CompletionResult(
    val payload: CompletionPayload,
    val aggregates: CompletionAggregates,
)

fun buildCompletionPayload(
    state: ActiveWorkout,
    now: Long = System.currentTimeMillis(),
): CompletionResult {
    val completedAt = isoFromMillis(now)
    val durationSeconds = maxOf(0L, (now - millisFromIso(state.startedAt)) / 1000L).toInt()

    var totalVolume = 0.0
    var completedSets = 0
    var plannedWorkingSets = 0

    val exercises = mutableListOf<CompletionExercisePayload>()
    for (ex in state.exercises) {
        plannedWorkingSets += ex.sets.count { !it.isWarmup }
        val done = ex.sets.filter { it.isCompleted }
        if (done.isEmpty()) continue

        val sets = done.mapIndexed { i, s ->
            if (!s.isWarmup) {
                completedSets += 1
                totalVolume += (s.weightKg ?: 0.0) * (s.reps ?: 0)
            }
            CompletionSetPayload(
                setNumber = i + 1,
                weightKg = s.weightKg,
                reps = s.reps,
                rpe = s.rpe,
                isWarmup = s.isWarmup,
                isCompleted = true,
                estimated1rmKg = if (s.isWarmup) null else estimatedOneRepMax(s.weightKg, s.reps),
                completedAt = s.completedAt,
            )
        }

        exercises.add(
            CompletionExercisePayload(
                exerciseId = ex.exerciseId,
                orderIndex = exercises.size,
                notes = ex.notes.trim().ifEmpty { null },
                exerciseScore = null,
                performanceGrade = null,
                sets = sets,
            ),
        )
    }

    val totalVolumeKg = round2(totalVolume)

    return CompletionResult(
        payload = CompletionPayload(
            session = CompletionSession(
                id = state.sessionId,
                templateId = state.templateId,
                name = state.name,
                gateDifficulty = null,
                startedAt = state.startedAt,
                completedAt = completedAt,
                durationSeconds = durationSeconds,
                totalVolumeKg = totalVolumeKg,
                completionScore = null,
                progressScore = null,
                qualityScore = null,
                gateScore = null,
                gateClearRank = null,
                xpEarned = null,
            ),
            exercises = exercises,
        ),
        aggregates = CompletionAggregates(
            name = state.name,
            gateDifficulty = null,
            durationSeconds = durationSeconds,
            totalVolumeKg = totalVolumeKg,
            completedSets = completedSets,
            plannedWorkingSets = plannedWorkingSets,
            exerciseCount = exercises.size,
        ),
    )
}

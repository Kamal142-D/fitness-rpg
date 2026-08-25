package com.fitnessrpg.app.data.workout

import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.pr.DetectExercise
import com.fitnessrpg.app.domain.pr.DetectSet
import com.fitnessrpg.app.domain.pr.DetectedPR
import com.fitnessrpg.app.domain.pr.detectPRs
import com.fitnessrpg.app.domain.pr.prioritizePRs
import com.fitnessrpg.app.domain.progression.CurrentAttributes
import com.fitnessrpg.app.domain.progression.ProgressionSnapshot
import com.fitnessrpg.app.domain.progression.ProgressionUpdateInput
import com.fitnessrpg.app.domain.progression.StreakSnapshot
import com.fitnessrpg.app.domain.progression.buildProgressionUpdate
import com.fitnessrpg.app.domain.progression.computeWorkoutAttributes
import com.fitnessrpg.app.domain.ranking.DayOutcome
import com.fitnessrpg.app.domain.ranking.StreakState
import com.fitnessrpg.app.domain.ranking.updateStreak
import com.fitnessrpg.app.domain.workouts.CompletionAggregates
import com.fitnessrpg.app.domain.workouts.CompletionPayload
import com.fitnessrpg.app.domain.workouts.GateResult
import com.fitnessrpg.app.domain.workouts.computeGateResult
import com.fitnessrpg.app.domain.gates.DifficultySet
import com.fitnessrpg.app.domain.gates.ExerciseDifficultyInput
import com.fitnessrpg.app.domain.gates.calculateGateDifficulty

data class FinishResult(
    val sessionId: String,
    val aggregates: CompletionAggregates,
    val prs: List<DetectedPR>,
    val gate: GateResult,
    val exerciseNames: Map<String, String> = emptyMap(),
)

/** Holds the most recent finish result so the completion screen can display it. */
object WorkoutResultHolder {
    var last: FinishResult? = null
}

/**
 * Finish a workout end-to-end (mirrors the RN useFinishWorkout): fetch prior
 * bests, detect PRs, compute the Gate result (all pure), persist the session via
 * the atomic RPC, then best-effort apply PRs, quests, and durable progression.
 * The workout is saved once completion succeeds; the rest never blocks it.
 */
class FinishWorkoutUseCase {

    suspend fun finish(
        userId: String,
        payload: CompletionPayload,
        aggregates: CompletionAggregates,
    ): FinishResult {
        val exerciseIds = payload.exercises.map { it.exerciseId }.distinct()

        val priorStats = runCatching {
            ServiceLocator.prRepository.getExerciseStats(exerciseIds)
        }.getOrDefault(emptyMap())

        val gateBaselines = runCatching {
            ServiceLocator.workoutRepository.getExerciseGateBaselines(exerciseIds)
        }.getOrDefault(emptyMap())
        val priorTemplateVolume = runCatching {
            ServiceLocator.workoutRepository.getPriorTemplateVolume(payload.session.templateId)
        }.getOrNull()

        val exerciseMetadata = runCatching { ServiceLocator.gateRepository.getExercises(exerciseIds) }.getOrDefault(emptyMap())
        val bodyWeightKg = runCatching { ServiceLocator.profileRepository.getProfile(userId)?.currentWeightKg }.getOrNull()
        val difficulty = calculateGateDifficulty(payload.exercises.map { ex ->
            val meta = exerciseMetadata[ex.exerciseId]
            val recent = gateBaselines[ex.exerciseId]
            ExerciseDifficultyInput(
                exerciseId = ex.exerciseId,
                sets = ex.sets.map { DifficultySet(it.weightKg, it.reps, it.rpe, it.isWarmup) },
                currentEstimated1rmKg = recent?.recentBest1rmKg ?: priorStats[ex.exerciseId]?.bestEstimated1rmKg,
                bodyWeightKg = bodyWeightKg,
                equipment = meta?.equipment,
                isMajorExercise = meta?.category !in setOf("arms", "core"),
                recentAverageVolumeKg = recent?.averageVolumeKg,
                priorSessionCount = recent?.sessionCount ?: 0,
            )
        }, workoutDurationMinutes = aggregates.durationSeconds / 60.0)

        val detectExercises = payload.exercises.map { ex ->
            DetectExercise(
                exerciseId = ex.exerciseId,
                orderIndex = ex.orderIndex,
                sets = ex.sets.map { DetectSet(it.setNumber, it.weightKg, it.reps, it.estimated1rmKg, it.isWarmup) },
            )
        }
        val detection = detectPRs(detectExercises, priorStats)
        val gate = computeGateResult(payload, priorStats, aggregates, detection.prs.map { it.recordType }, difficulty, priorTemplateVolume)

        val sessionId = ServiceLocator.workoutRepository.completeWorkout(augment(payload, gate))

        runCatching { ServiceLocator.prRepository.applyWorkoutResults(sessionId, detection.prs, detection.stats) }
        runCatching { ServiceLocator.questRepository.recordWorkoutForQuests(sessionId) }
        runCatching {
            val prog = ServiceLocator.progressionRepository.getProgression(userId)
            if (prog != null) {
                val newStreak = updateStreak(
                    StreakState(prog.currentStreakDays, prog.longestStreakDays),
                    DayOutcome(didTrain = true, isScheduledRest = false),
                )
                val snapshot = buildProgressionUpdate(
                    ProgressionUpdateInput(
                        current = ProgressionSnapshot(prog.level, prog.currentXp, prog.lifetimeXp),
                        currentAttributes = CurrentAttributes(prog.strengthScore, prog.physiqueScore, prog.enduranceScore, prog.disciplineScore, prog.hunterScore, prog.hunterRank),
                        xpEarned = gate.xpEarned,
                        streak = StreakSnapshot(newStreak.current, newStreak.longest),
                        attributes = computeWorkoutAttributes(newStreak.current),
                    ),
                )
                ServiceLocator.progressionRepository.applySessionProgression(sessionId, snapshot)
            }
        }
        // Recalculate from the newly persisted, recent validated evidence. The
        // strength engine uses multiple sessions for A/S and ignores lifetime PRs.
        runCatching { ServiceLocator.assessmentRepository.recalculateAndPersist(userId) }

        return FinishResult(sessionId, aggregates, prioritizePRs(detection.prs), gate, exerciseMetadata.mapValues { it.value.name })
    }

    /** Attach the computed Gate result to the session + per-exercise payload. */
    private fun augment(payload: CompletionPayload, gate: GateResult): CompletionPayload =
        payload.copy(
            session = payload.session.copy(
                completionScore = gate.completionScore,
                progressScore = gate.progressScore,
                qualityScore = gate.qualityScore,
                gateScore = gate.gateScore,
                gateClearRank = gate.gateClearRank.name,
                xpEarned = gate.xpEarned,
                gateDifficultyScore = gate.difficulty?.score,
                gateDifficultyRank = gate.difficulty?.rank?.name,
            ),
            exercises = payload.exercises.mapIndexed { i, ex ->
                ex.copy(
                    exerciseScore = gate.perExercise.getOrNull(i)?.performanceScore,
                    performanceGrade = gate.perExercise.getOrNull(i)?.performanceGrade?.name,
                    difficultyScore = gate.difficulty?.perExercise?.find { it.exerciseId == ex.exerciseId }?.score,
                    difficultyRank = gate.difficulty?.perExercise?.find { it.exerciseId == ex.exerciseId }?.rank?.name,
                )
            },
        )
}

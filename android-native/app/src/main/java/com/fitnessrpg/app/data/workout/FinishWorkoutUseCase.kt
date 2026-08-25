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
import com.fitnessrpg.app.domain.progression.computeAttributes
import com.fitnessrpg.app.domain.ranking.DayOutcome
import com.fitnessrpg.app.domain.ranking.StreakState
import com.fitnessrpg.app.domain.ranking.updateStreak
import com.fitnessrpg.app.domain.workouts.CompletionAggregates
import com.fitnessrpg.app.domain.workouts.CompletionPayload
import com.fitnessrpg.app.domain.workouts.GateResult
import com.fitnessrpg.app.domain.workouts.computeGateResult

data class FinishResult(
    val sessionId: String,
    val aggregates: CompletionAggregates,
    val prs: List<DetectedPR>,
    val gate: GateResult,
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

        val detectExercises = payload.exercises.map { ex ->
            DetectExercise(
                exerciseId = ex.exerciseId,
                orderIndex = ex.orderIndex,
                sets = ex.sets.map { DetectSet(it.setNumber, it.weightKg, it.reps, it.estimated1rmKg, it.isWarmup) },
            )
        }
        val detection = detectPRs(detectExercises, priorStats)
        val gate = computeGateResult(payload, priorStats, aggregates, detection.prs.map { it.recordType })

        val sessionId = ServiceLocator.workoutRepository.completeWorkout(augment(payload, gate))

        runCatching { ServiceLocator.prRepository.applyWorkoutResults(sessionId, detection.prs, detection.stats) }
        runCatching { ServiceLocator.questRepository.recordWorkoutForQuests(sessionId) }
        runCatching {
            val prog = ServiceLocator.progressionRepository.getProgression(userId)
            if (prog != null) {
                val inputs = ServiceLocator.progressionRepository.getFinishInputs(userId)
                val newStreak = updateStreak(
                    StreakState(prog.currentStreakDays, prog.longestStreakDays),
                    DayOutcome(didTrain = true, isScheduledRest = false),
                )
                val snapshot = buildProgressionUpdate(
                    ProgressionUpdateInput(
                        current = ProgressionSnapshot(prog.level, prog.currentXp, prog.lifetimeXp),
                        currentAttributes = CurrentAttributes(prog.strengthScore, prog.physiqueScore, prog.enduranceScore, prog.disciplineScore),
                        xpEarned = gate.xpEarned,
                        streak = StreakSnapshot(newStreak.current, newStreak.longest),
                        attributes = computeAttributes(inputs, newStreak.current),
                    ),
                )
                ServiceLocator.progressionRepository.applySessionProgression(sessionId, snapshot)
            }
        }

        return FinishResult(sessionId, aggregates, prioritizePRs(detection.prs), gate)
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
            ),
            exercises = payload.exercises.mapIndexed { i, ex ->
                ex.copy(
                    exerciseScore = gate.perExercise.getOrNull(i)?.performanceScore,
                    performanceGrade = gate.perExercise.getOrNull(i)?.performanceGrade?.name,
                )
            },
        )
}

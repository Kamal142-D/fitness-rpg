package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.gates.templateDifficulty
import com.fitnessrpg.app.domain.model.ActiveExercise
import com.fitnessrpg.app.domain.model.ActiveSet
import com.fitnessrpg.app.domain.model.ActiveWorkout
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.util.genId
import com.fitnessrpg.app.util.isoFromMillis
import com.fitnessrpg.app.util.uuidV4

/**
 * Pure reducers for the active workout. No storage — the persisted store wraps
 * these. All functions return a NEW state; inputs are never mutated.
 */
private const val DEFAULT_SETS = 3

/** Build a fresh active workout from a Gate detail. */
fun createActiveWorkout(detail: GateDetail, now: Long = System.currentTimeMillis()): ActiveWorkout {
    val exercises = detail.exercises.map { twe ->
        val te = twe.templateExercise
        val ex = twe.exercise
        val count = maxOf(1, te.targetSets ?: DEFAULT_SETS)
        val sets = (0 until count).map { i ->
            ActiveSet(
                id = genId("set"),
                setNumber = i + 1,
                weightKg = null,
                reps = te.targetRepsMin, // smart default: target minimum reps
                rpe = null,
                isWarmup = false,
                isCompleted = false,
                completedAt = null,
            )
        }
        ActiveExercise(
            id = genId("ex"),
            exerciseId = te.exerciseId,
            name = ex.name,
            primaryMuscle = ex.primaryMuscleGroup,
            targetSets = te.targetSets,
            targetRepsMin = te.targetRepsMin,
            targetRepsMax = te.targetRepsMax,
            targetRpe = te.targetRpe,
            restSeconds = te.restSeconds,
            rankingEnabled = ex.rankingEnabled,
            notes = "",
            sets = sets,
        )
    }

    return ActiveWorkout(
        sessionId = uuidV4(),
        templateId = detail.template.id,
        name = detail.template.name,
        gateDifficulty = null,
        startedAt = isoFromMillis(now),
        currentExerciseIndex = 0,
        restEndsAt = null,
        exercises = exercises,
    )
}

private fun ActiveWorkout.mapExercise(exIdx: Int, fn: (ActiveExercise) -> ActiveExercise): ActiveWorkout {
    if (exIdx < 0 || exIdx >= exercises.size) return this
    return copy(exercises = exercises.mapIndexed { i, ex -> if (i == exIdx) fn(ex) else ex })
}

private fun ActiveWorkout.mapSet(exIdx: Int, setIdx: Int, fn: (ActiveSet) -> ActiveSet): ActiveWorkout =
    mapExercise(exIdx) { ex ->
        if (setIdx < 0 || setIdx >= ex.sets.size) ex
        else ex.copy(sets = ex.sets.mapIndexed { i, s -> if (i == setIdx) fn(s) else s })
    }

private fun renumber(sets: List<ActiveSet>): List<ActiveSet> =
    sets.mapIndexed { i, s -> s.copy(setNumber = i + 1) }

/**
 * A partial edit of a set's weight / reps / rpe. A null field means "leave
 * unchanged" (the UI never clears a value to null through this path).
 */
data class SetPatch(val weightKg: Double? = null, val reps: Int? = null, val rpe: Double? = null)

/** Edit a set's weight / reps / rpe (null field = leave unchanged). */
fun updateSet(state: ActiveWorkout, exIdx: Int, setIdx: Int, patch: SetPatch): ActiveWorkout =
    state.mapSet(exIdx, setIdx) { s ->
        s.copy(
            weightKg = patch.weightKg ?: s.weightKg,
            reps = patch.reps ?: s.reps,
            rpe = patch.rpe ?: s.rpe,
        )
    }

/** Set a set's weight directly (null clears it). */
fun setWeight(state: ActiveWorkout, exIdx: Int, setIdx: Int, weightKg: Double?): ActiveWorkout =
    state.mapSet(exIdx, setIdx) { it.copy(weightKg = weightKg) }

/** Set a set's reps directly (null clears it). */
fun setReps(state: ActiveWorkout, exIdx: Int, setIdx: Int, reps: Int?): ActiveWorkout =
    state.mapSet(exIdx, setIdx) { it.copy(reps = reps) }

/** Set a set's RPE directly (null clears it). */
fun setRpe(state: ActiveWorkout, exIdx: Int, setIdx: Int, rpe: Double?): ActiveWorkout =
    state.mapSet(exIdx, setIdx) { it.copy(rpe = rpe) }

/**
 * Mark a set complete: stamp it, start the rest timer from this exercise's rest
 * seconds, and pre-fill the next uncompleted set's weight/reps from this one.
 */
fun completeSet(
    state: ActiveWorkout,
    exIdx: Int,
    setIdx: Int,
    now: Long = System.currentTimeMillis(),
): ActiveWorkout {
    val ex = state.exercises.getOrNull(exIdx) ?: return state
    val set = ex.sets.getOrNull(setIdx) ?: return state

    var next = state.mapSet(exIdx, setIdx) { s ->
        s.copy(isCompleted = true, completedAt = isoFromMillis(now))
    }

    val following = next.exercises[exIdx].sets.getOrNull(setIdx + 1)
    if (following != null && !following.isCompleted) {
        next = next.mapSet(exIdx, setIdx + 1) { s ->
            s.copy(weightKg = s.weightKg ?: set.weightKg, reps = s.reps ?: set.reps)
        }
    }

    val restMs = (ex.restSeconds ?: 0) * 1000L
    return next.copy(restEndsAt = if (restMs > 0) now + restMs else null)
}

/** Revert a completed set back to incomplete. */
fun uncompleteSet(state: ActiveWorkout, exIdx: Int, setIdx: Int): ActiveWorkout =
    state.mapSet(exIdx, setIdx) { it.copy(isCompleted = false, completedAt = null) }

fun toggleWarmup(state: ActiveWorkout, exIdx: Int, setIdx: Int): ActiveWorkout =
    state.mapSet(exIdx, setIdx) { it.copy(isWarmup = !it.isWarmup) }

/** Append a set, copying the last set's weight/reps as a smart default. */
fun addSet(state: ActiveWorkout, exIdx: Int): ActiveWorkout =
    state.mapExercise(exIdx) { ex ->
        val last = ex.sets.lastOrNull()
        val set = ActiveSet(
            id = genId("set"),
            setNumber = ex.sets.size + 1,
            weightKg = last?.weightKg,
            reps = last?.reps,
            rpe = null,
            isWarmup = false,
            isCompleted = false,
            completedAt = null,
        )
        ex.copy(sets = ex.sets + set)
    }

/** Remove a set (keeping at least one) and renumber the rest. */
fun removeSet(state: ActiveWorkout, exIdx: Int, setIdx: Int): ActiveWorkout =
    state.mapExercise(exIdx) { ex ->
        if (ex.sets.size <= 1) ex
        else ex.copy(sets = renumber(ex.sets.filterIndexed { i, _ -> i != setIdx }))
    }

private fun activeExerciseFrom(exercise: Exercise): ActiveExercise = ActiveExercise(
    id = genId("ex"), exerciseId = exercise.id, name = exercise.name,
    primaryMuscle = exercise.primaryMuscleGroup, targetSets = DEFAULT_SETS,
    targetRepsMin = 8, targetRepsMax = 12, targetRpe = 8.0, restSeconds = 90,
    rankingEnabled = exercise.rankingEnabled, notes = "",
    sets = (1..DEFAULT_SETS).map { number ->
        ActiveSet(genId("set"), number, null, 8, null, false, false, null)
    },
)

fun addExercise(state: ActiveWorkout, exercise: Exercise): ActiveWorkout =
    if (state.exercises.any { it.exerciseId == exercise.id }) state
    else state.copy(exercises = state.exercises + activeExerciseFrom(exercise))

fun replaceExercise(state: ActiveWorkout, exIdx: Int, exercise: Exercise): ActiveWorkout =
    if (exIdx !in state.exercises.indices) state
    else state.copy(exercises = state.exercises.mapIndexed { index, current ->
        if (index == exIdx) activeExerciseFrom(exercise) else current
    })

fun setCurrentExercise(state: ActiveWorkout, idx: Int): ActiveWorkout {
    val clamped = maxOf(0, minOf(idx, state.exercises.size - 1))
    return state.copy(currentExerciseIndex = clamped)
}

fun setNotes(state: ActiveWorkout, exIdx: Int, notes: String): ActiveWorkout =
    state.mapExercise(exIdx) { it.copy(notes = notes) }

fun clearRest(state: ActiveWorkout): ActiveWorkout = state.copy(restEndsAt = null)

/** Extend (or start) the rest timer by [seconds] from now. */
fun addRestSeconds(
    state: ActiveWorkout,
    seconds: Int,
    now: Long = System.currentTimeMillis(),
): ActiveWorkout {
    val base = if (state.restEndsAt != null && state.restEndsAt > now) state.restEndsAt else now
    return state.copy(restEndsAt = base + seconds * 1000L)
}

/** Count of completed, non-warmup sets across the workout. */
fun completedWorkingSetCount(state: ActiveWorkout): Int =
    state.exercises.sumOf { ex -> ex.sets.count { it.isCompleted && !it.isWarmup } }

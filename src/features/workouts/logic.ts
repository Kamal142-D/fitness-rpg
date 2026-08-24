/**
 * Pure reducers for the active workout. No React, no storage — the persisted
 * Zustand store (useActiveWorkoutStore) wraps these, and tests exercise them
 * directly. All functions return a NEW state; inputs are never mutated.
 */
import { templateDifficulty } from '@/features/gates/mappers';
import type { GateDetail } from '@/features/gates/types';
import type { ActiveExercise, ActiveSet, ActiveWorkout } from '@/features/workouts/types';
import { genId } from '@/utils/id';
import { uuidv4 } from '@/utils/uuid';

const DEFAULT_SETS = 3;

/** Build a fresh active workout from a Gate detail. */
export function createActiveWorkout(detail: GateDetail, now: number = Date.now()): ActiveWorkout {
  const exercises: ActiveExercise[] = detail.exercises.map((te) => {
    const count = Math.max(1, te.target_sets ?? DEFAULT_SETS);
    const sets: ActiveSet[] = Array.from({ length: count }, (_, i) => ({
      id: genId('set'),
      setNumber: i + 1,
      weightKg: null,
      reps: te.target_reps_min ?? null, // smart default: target minimum reps
      rpe: null,
      isWarmup: false,
      isCompleted: false,
      completedAt: null,
    }));
    return {
      id: genId('ex'),
      exerciseId: te.exercise_id,
      name: te.exercise.name,
      primaryMuscle: te.exercise.primary_muscle_group,
      targetSets: te.target_sets,
      targetRepsMin: te.target_reps_min,
      targetRepsMax: te.target_reps_max,
      targetRpe: te.target_rpe,
      restSeconds: te.rest_seconds,
      rankingEnabled: te.exercise.ranking_enabled,
      notes: '',
      sets,
    };
  });

  return {
    sessionId: uuidv4(),
    templateId: detail.template.id,
    name: detail.template.name,
    gateDifficulty: templateDifficulty(detail.template),
    startedAt: new Date(now).toISOString(),
    currentExerciseIndex: 0,
    restEndsAt: null,
    exercises,
  };
}

function mapExercise(
  state: ActiveWorkout,
  exIdx: number,
  fn: (ex: ActiveExercise) => ActiveExercise,
): ActiveWorkout {
  if (exIdx < 0 || exIdx >= state.exercises.length) return state;
  const exercises = state.exercises.map((ex, i) => (i === exIdx ? fn(ex) : ex));
  return { ...state, exercises };
}

function mapSet(
  state: ActiveWorkout,
  exIdx: number,
  setIdx: number,
  fn: (set: ActiveSet) => ActiveSet,
): ActiveWorkout {
  return mapExercise(state, exIdx, (ex) => {
    if (setIdx < 0 || setIdx >= ex.sets.length) return ex;
    return { ...ex, sets: ex.sets.map((s, i) => (i === setIdx ? fn(s) : s)) };
  });
}

function renumber(sets: ActiveSet[]): ActiveSet[] {
  return sets.map((s, i) => ({ ...s, setNumber: i + 1 }));
}

/** Edit a set's weight / reps / rpe. */
export function updateSet(
  state: ActiveWorkout,
  exIdx: number,
  setIdx: number,
  patch: Partial<Pick<ActiveSet, 'weightKg' | 'reps' | 'rpe'>>,
): ActiveWorkout {
  return mapSet(state, exIdx, setIdx, (s) => ({ ...s, ...patch }));
}

/**
 * Mark a set complete: stamp it, start the rest timer from this exercise's rest
 * seconds, and pre-fill the next uncompleted set's weight/reps from this one.
 */
export function completeSet(
  state: ActiveWorkout,
  exIdx: number,
  setIdx: number,
  now: number = Date.now(),
): ActiveWorkout {
  const ex = state.exercises[exIdx];
  if (!ex) return state;
  const set = ex.sets[setIdx];
  if (!set) return state;

  let next = mapSet(state, exIdx, setIdx, (s) => ({
    ...s,
    isCompleted: true,
    completedAt: new Date(now).toISOString(),
  }));

  const following = next.exercises[exIdx].sets[setIdx + 1];
  if (following && !following.isCompleted) {
    next = mapSet(next, exIdx, setIdx + 1, (s) => ({
      ...s,
      weightKg: s.weightKg ?? set.weightKg,
      reps: s.reps ?? set.reps,
    }));
  }

  const restMs = (ex.restSeconds ?? 0) * 1000;
  return { ...next, restEndsAt: restMs > 0 ? now + restMs : null };
}

/** Revert a completed set back to incomplete. */
export function uncompleteSet(state: ActiveWorkout, exIdx: number, setIdx: number): ActiveWorkout {
  return mapSet(state, exIdx, setIdx, (s) => ({ ...s, isCompleted: false, completedAt: null }));
}

export function toggleWarmup(state: ActiveWorkout, exIdx: number, setIdx: number): ActiveWorkout {
  return mapSet(state, exIdx, setIdx, (s) => ({ ...s, isWarmup: !s.isWarmup }));
}

/** Append a set, copying the last set's weight/reps as a smart default. */
export function addSet(state: ActiveWorkout, exIdx: number): ActiveWorkout {
  return mapExercise(state, exIdx, (ex) => {
    const last = ex.sets[ex.sets.length - 1];
    const set: ActiveSet = {
      id: genId('set'),
      setNumber: ex.sets.length + 1,
      weightKg: last?.weightKg ?? null,
      reps: last?.reps ?? null,
      rpe: null,
      isWarmup: false,
      isCompleted: false,
      completedAt: null,
    };
    return { ...ex, sets: [...ex.sets, set] };
  });
}

/** Remove a set (keeping at least one) and renumber the rest. */
export function removeSet(state: ActiveWorkout, exIdx: number, setIdx: number): ActiveWorkout {
  return mapExercise(state, exIdx, (ex) => {
    if (ex.sets.length <= 1) return ex;
    return { ...ex, sets: renumber(ex.sets.filter((_, i) => i !== setIdx)) };
  });
}

export function setCurrentExercise(state: ActiveWorkout, idx: number): ActiveWorkout {
  const clamped = Math.max(0, Math.min(idx, state.exercises.length - 1));
  return { ...state, currentExerciseIndex: clamped };
}

export function setNotes(state: ActiveWorkout, exIdx: number, notes: string): ActiveWorkout {
  return mapExercise(state, exIdx, (ex) => ({ ...ex, notes }));
}

export function clearRest(state: ActiveWorkout): ActiveWorkout {
  return { ...state, restEndsAt: null };
}

/** Extend (or start) the rest timer by `seconds` from now. */
export function addRestSeconds(
  state: ActiveWorkout,
  seconds: number,
  now: number = Date.now(),
): ActiveWorkout {
  const base = state.restEndsAt && state.restEndsAt > now ? state.restEndsAt : now;
  return { ...state, restEndsAt: base + seconds * 1000 };
}

/** Count of completed, non-warmup sets across the workout. */
export function completedWorkingSetCount(state: ActiveWorkout): number {
  return state.exercises.reduce(
    (acc, ex) => acc + ex.sets.filter((s) => s.isCompleted && !s.isWarmup).length,
    0,
  );
}

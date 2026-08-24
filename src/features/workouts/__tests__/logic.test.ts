import type { Exercise, GateDetail, GateTemplate, TemplateExercise } from '@/features/gates/types';
import {
  addSet,
  completeSet,
  completedWorkingSetCount,
  createActiveWorkout,
  removeSet,
  setCurrentExercise,
  toggleWarmup,
  updateSet,
} from '@/features/workouts/logic';

function detail(): GateDetail {
  const template: GateTemplate = {
    id: 'tpl1',
    user_id: null,
    name: 'Push',
    description: 'Chest, shoulders',
    estimated_duration_minutes: 50,
    difficulty: 'C',
    is_system_template: true,
    created_at: '',
    updated_at: '',
  };
  const mkEx = (
    exId: string,
    name: string,
    sets: number,
    rest: number,
  ): TemplateExercise & { exercise: Exercise } => ({
    id: `te_${exId}`,
    template_id: 'tpl1',
    exercise_id: exId,
    order_index: 0,
    target_sets: sets,
    target_reps_min: 5,
    target_reps_max: 8,
    target_rpe: 8,
    rest_seconds: rest,
    exercise: {
      id: exId,
      name,
      category: 'chest',
      primary_muscle_group: 'chest',
      secondary_muscle_groups: [],
      equipment: 'barbell',
      exercise_type: 'strength',
      ranking_enabled: true,
      created_at: '',
    },
  });
  return { template, exercises: [mkEx('ex1', 'Bench', 2, 120), mkEx('ex2', 'OHP', 2, 90)] };
}

describe('createActiveWorkout', () => {
  it('builds exercises + target-count sets with smart rep defaults', () => {
    const w = createActiveWorkout(detail(), 0);
    expect(w.exercises).toHaveLength(2);
    expect(w.exercises[0].sets).toHaveLength(2);
    expect(w.exercises[0].sets[0].reps).toBe(5); // target_reps_min
    expect(w.exercises[0].sets[0].weightKg).toBeNull();
    expect(w.gateDifficulty).toBe('C');
    expect(w.sessionId).toMatch(/[0-9a-f-]{36}/);
  });
});

describe('completeSet', () => {
  it('stamps the set, starts the rest timer, and pre-fills the next set', () => {
    let w = createActiveWorkout(detail(), 0);
    w = updateSet(w, 0, 0, { weightKg: 100, reps: 5 });
    w = completeSet(w, 0, 0, 10_000);
    expect(w.exercises[0].sets[0].isCompleted).toBe(true);
    expect(w.restEndsAt).toBe(10_000 + 120 * 1000);
    expect(w.exercises[0].sets[1].weightKg).toBe(100); // pre-filled from previous
  });
});

describe('updateSet immutability', () => {
  it('does not mutate the input state', () => {
    const w = createActiveWorkout(detail(), 0);
    const next = updateSet(w, 0, 0, { weightKg: 80 });
    expect(w.exercises[0].sets[0].weightKg).toBeNull();
    expect(next.exercises[0].sets[0].weightKg).toBe(80);
  });
});

describe('addSet / removeSet', () => {
  it('appends copying the last set and renumbers on remove', () => {
    let w = createActiveWorkout(detail(), 0);
    w = updateSet(w, 0, 1, { weightKg: 90, reps: 6 });
    w = addSet(w, 0);
    expect(w.exercises[0].sets).toHaveLength(3);
    expect(w.exercises[0].sets[2].weightKg).toBe(90);
    w = removeSet(w, 0, 1);
    expect(w.exercises[0].sets).toHaveLength(2);
    expect(w.exercises[0].sets.map((s) => s.setNumber)).toEqual([1, 2]);
  });
  it('will not remove the last remaining set', () => {
    let w = createActiveWorkout(detail(), 0);
    w = removeSet(w, 0, 0);
    w = removeSet(w, 0, 0);
    expect(w.exercises[0].sets.length).toBeGreaterThanOrEqual(1);
  });
});

describe('toggleWarmup, navigation, counts', () => {
  it('toggles warm-up and clamps exercise navigation', () => {
    let w = createActiveWorkout(detail(), 0);
    w = toggleWarmup(w, 0, 0);
    expect(w.exercises[0].sets[0].isWarmup).toBe(true);
    w = setCurrentExercise(w, 99);
    expect(w.currentExerciseIndex).toBe(1);
    w = setCurrentExercise(w, -5);
    expect(w.currentExerciseIndex).toBe(0);
  });
  it('counts completed working sets only', () => {
    let w = createActiveWorkout(detail(), 0);
    w = completeSet(w, 0, 0, 1000); // working
    w = toggleWarmup(w, 0, 1);
    w = completeSet(w, 0, 1, 2000); // warmup
    expect(completedWorkingSetCount(w)).toBe(1);
  });
});

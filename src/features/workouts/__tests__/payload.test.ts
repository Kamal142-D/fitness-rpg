import type { Exercise, GateDetail, GateTemplate, TemplateExercise } from '@/features/gates/types';
import {
  completeSet,
  createActiveWorkout,
  toggleWarmup,
  updateSet,
} from '@/features/workouts/logic';
import { buildCompletionPayload } from '@/features/workouts/payload';

function detail(): GateDetail {
  const template: GateTemplate = {
    id: 'tpl1',
    user_id: null,
    name: 'Push',
    description: 'Chest',
    estimated_duration_minutes: 50,
    difficulty: 'C',
    is_system_template: true,
    created_at: '',
    updated_at: '',
  };
  const mkEx = (exId: string, name: string): TemplateExercise & { exercise: Exercise } => ({
    id: `te_${exId}`,
    template_id: 'tpl1',
    exercise_id: exId,
    order_index: 0,
    target_sets: 2,
    target_reps_min: 5,
    target_reps_max: 8,
    target_rpe: 8,
    rest_seconds: 90,
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
  return { template, exercises: [mkEx('ex1', 'Bench'), mkEx('ex2', 'OHP')] };
}

describe('buildCompletionPayload', () => {
  it('persists only completed sets, only exercises with any, and computes aggregates', () => {
    let w = createActiveWorkout(detail(), 0); // startedAt = epoch 0
    // ex1: a warm-up set then a working set, both completed
    w = updateSet(w, 0, 0, { weightKg: 60, reps: 5 });
    w = toggleWarmup(w, 0, 0);
    w = completeSet(w, 0, 0, 1000);
    w = updateSet(w, 0, 1, { weightKg: 100, reps: 5 });
    w = completeSet(w, 0, 1, 2000);
    // ex2: nothing completed

    const { payload, aggregates } = buildCompletionPayload(w, 60_000);

    expect(payload.exercises).toHaveLength(1); // ex2 omitted
    expect(payload.exercises[0].sets).toHaveLength(2);
    expect(payload.exercises[0].sets.map((s) => s.set_number)).toEqual([1, 2]);

    // Volume + count exclude the warm-up.
    expect(aggregates.totalVolumeKg).toBe(500); // 100 * 5
    expect(aggregates.completedSets).toBe(1);
    expect(aggregates.exerciseCount).toBe(1);
    expect(aggregates.durationSeconds).toBe(60);

    // est-1RM: working set computed, warm-up left null.
    const working = payload.exercises[0].sets.find((s) => !s.is_warmup)!;
    const warm = payload.exercises[0].sets.find((s) => s.is_warmup)!;
    expect(working.estimated_1rm_kg).toBe(116.67);
    expect(warm.estimated_1rm_kg).toBeNull();

    // Session carries the idempotency id + completed status fields.
    expect(payload.session.id).toBe(w.sessionId);
    expect(payload.session.total_volume_kg).toBe(500);
  });

  it('produces no exercises when nothing was completed', () => {
    const w = createActiveWorkout(detail(), 0);
    const { payload, aggregates } = buildCompletionPayload(w, 1000);
    expect(payload.exercises).toHaveLength(0);
    expect(aggregates.completedSets).toBe(0);
  });
});

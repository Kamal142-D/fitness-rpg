import type { PriorStat } from '@/features/pr/types';
import { computeGateResult } from '@/features/workouts/gateResult';
import type { CompletionAggregates, CompletionPayload } from '@/features/workouts/payload';

function workingSet(setNumber: number) {
  return {
    set_number: setNumber,
    weight_kg: 100,
    reps: 5,
    rpe: null,
    is_warmup: false,
    is_completed: true,
    estimated_1rm_kg: 116.67,
    completed_at: null,
  };
}

function payload(): CompletionPayload {
  return {
    session: {
      id: 's',
      template_id: 't',
      name: 'Push',
      gate_difficulty: 'C',
      started_at: '',
      completed_at: '',
      duration_seconds: 0,
      total_volume_kg: 1000,
      completion_score: null,
      progress_score: null,
      quality_score: null,
      gate_score: null,
      gate_clear_rank: null,
      xp_earned: null,
    },
    exercises: [
      {
        exercise_id: 'ex1',
        order_index: 0,
        notes: null,
        exercise_score: null,
        performance_grade: null,
        sets: [workingSet(1), workingSet(2)],
      },
    ],
  };
}

const aggregates: CompletionAggregates = {
  name: 'Push',
  gateDifficulty: 'C',
  durationSeconds: 0,
  totalVolumeKg: 1000,
  completedSets: 2,
  plannedWorkingSets: 2,
  exerciseCount: 1,
};

describe('computeGateResult', () => {
  it('computes gate score, clear rank, grades and XP with no history (neutral)', () => {
    const priorStats: Record<string, PriorStat | undefined> = {};
    const r = computeGateResult(payload(), priorStats, aggregates, []);

    expect(r.completionScore).toBe(100);
    // performance neutral (60), completion 100, pr 50, quality 60, progress omitted
    // (60*.5 + 100*.2 + 50*.1 + 60*.05) / .85 = 58 / .85 ≈ 68.2 -> A
    expect(r.gateScore).toBeCloseTo(68.24, 1);
    expect(r.gateClearRank).toBe('A');
    expect(r.perExercise[0].performanceGrade).toBe('B'); // neutral 60 -> B
    // xp: 300 base + 2*10 sets + 0 PRs + 250 (A bonus) = 570
    expect(r.xpEarned).toBe(570);
  });

  it('rewards meaningful PRs in the XP total', () => {
    const r = computeGateResult(payload(), {}, aggregates, [{ recordType: 'estimated_1rm' }]);
    // +50 for the meaningful PR, and the PR component nudges the gate score up.
    expect(r.xpEarned).toBeGreaterThan(570);
  });
});

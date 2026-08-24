import { detectPRs, prioritizePRs } from '@/features/pr/detect';
import type { DetectExercise, DetectedPR, PriorStat } from '@/features/pr/types';

function ex(sets: DetectExercise['sets'], exerciseId = 'ex1', orderIndex = 0): DetectExercise {
  return { exerciseId, orderIndex, sets };
}
function set(
  setNumber: number,
  weightKg: number | null,
  reps: number | null,
  est1RM: number | null,
  isWarmup = false,
) {
  return { setNumber, weightKg, reps, est1RM, isWarmup };
}

describe('detectPRs — baseline vs PR', () => {
  it('treats a first-ever attempt as a baseline (no PRs), but records stats', () => {
    const { prs, stats } = detectPRs([ex([set(1, 100, 5, 116.67)])], {});
    expect(prs).toHaveLength(0);
    expect(stats[0]).toMatchObject({
      exerciseId: 'ex1',
      bestWeightKg: 100,
      bestReps: 5,
      bestEstimated1rmKg: 116.67,
      bestVolumeKg: 500,
    });
  });

  it('flags a weight PR when it beats the prior best', () => {
    const prior: Record<string, PriorStat> = {
      ex1: { bestWeightKg: 100, bestReps: 8, bestEstimated1rmKg: 130, bestVolumeKg: 1000 },
    };
    const { prs } = detectPRs([ex([set(1, 110, 5, 128)])], prior);
    const weightPR = prs.find((p) => p.recordType === 'weight');
    expect(weightPR).toMatchObject({ previousValue: 100, newValue: 110, setNumber: 1 });
    // reps (5<8), 1rm (128<130), volume (550<1000) are not PRs
    expect(prs.map((p) => p.recordType)).toEqual(['weight']);
  });

  it('does not flag a PR when equal to or below the prior best', () => {
    const prior: Record<string, PriorStat> = {
      ex1: { bestWeightKg: 100, bestReps: 5, bestEstimated1rmKg: 116.67, bestVolumeKg: 500 },
    };
    const { prs } = detectPRs([ex([set(1, 100, 5, 116.67)])], prior);
    expect(prs).toHaveLength(0);
  });
});

describe('detectPRs — dedup, warm-ups, multiple types', () => {
  it('emits at most one PR per type per exercise (the best set)', () => {
    const prior: Record<string, PriorStat> = {
      ex1: { bestWeightKg: 90, bestReps: 4, bestEstimated1rmKg: 100, bestVolumeKg: 400 },
    };
    const { prs } = detectPRs([ex([set(1, 95, 6, 105), set(2, 100, 8, 120)])], prior);
    const weightPRs = prs.filter((p) => p.recordType === 'weight');
    expect(weightPRs).toHaveLength(1);
    expect(weightPRs[0].newValue).toBe(100); // the heavier set
    expect(weightPRs[0].setNumber).toBe(2);
  });

  it('ignores warm-up sets entirely', () => {
    const prior: Record<string, PriorStat> = {
      ex1: { bestWeightKg: 100, bestReps: 5, bestEstimated1rmKg: 116, bestVolumeKg: 500 },
    };
    // A very heavy warm-up must not create a weight PR.
    const { prs } = detectPRs([ex([set(1, 200, 1, 200, true), set(2, 90, 5, 105)])], prior);
    expect(prs.find((p) => p.recordType === 'weight')).toBeUndefined();
  });
});

describe('prioritizePRs', () => {
  const prs: DetectedPR[] = [
    {
      exerciseId: 'a',
      orderIndex: 0,
      setNumber: 1,
      recordType: 'volume',
      previousValue: 100,
      newValue: 150,
    },
    {
      exerciseId: 'a',
      orderIndex: 0,
      setNumber: 1,
      recordType: 'estimated_1rm',
      previousValue: 100,
      newValue: 110,
    },
    {
      exerciseId: 'a',
      orderIndex: 0,
      setNumber: 1,
      recordType: 'weight',
      previousValue: 100,
      newValue: 105,
    },
  ];

  it('orders estimated_1rm first, then weight, then volume', () => {
    expect(prioritizePRs(prs).map((p) => p.recordType)).toEqual([
      'estimated_1rm',
      'weight',
      'volume',
    ]);
  });

  it('caps the count when a limit is given', () => {
    expect(prioritizePRs(prs, 2)).toHaveLength(2);
  });
});

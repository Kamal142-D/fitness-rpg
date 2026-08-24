import {
  exerciseScore,
  nextExerciseRank,
  permanentExerciseRank,
} from '@/services/ranking/exerciseRank';

describe('exerciseScore', () => {
  it('maps a 1x-bodyweight bench (male) to the middle of the scale', () => {
    // Male bench anchor: ratio 1.0 -> score 50.
    const score = exerciseScore({
      exerciseName: 'Barbell Bench Press',
      bestEstimated1rmKg: 80,
      bodyweightKg: 80,
      sex: 'male',
    });
    expect(score).toBe(50);
    expect(permanentExerciseRank(score!)).toBe('B');
  });

  it('returns null for exercises with no strength standard', () => {
    expect(
      exerciseScore({
        exerciseName: 'Lateral Raise',
        bestEstimated1rmKg: 30,
        bodyweightKg: 80,
        sex: 'male',
      }),
    ).toBeNull();
  });

  it('returns null when bodyweight or 1RM is missing', () => {
    expect(
      exerciseScore({
        exerciseName: 'Barbell Bench Press',
        bestEstimated1rmKg: null,
        bodyweightKg: 80,
        sex: 'male',
      }),
    ).toBeNull();
    expect(
      exerciseScore({
        exerciseName: 'Barbell Bench Press',
        bestEstimated1rmKg: 80,
        bodyweightKg: 0,
        sex: 'male',
      }),
    ).toBeNull();
  });

  it('scales lighter for female standards (same absolute lift scores higher)', () => {
    const male = exerciseScore({
      exerciseName: 'Barbell Bench Press',
      bestEstimated1rmKg: 80,
      bodyweightKg: 80,
      sex: 'male',
    })!;
    const female = exerciseScore({
      exerciseName: 'Barbell Bench Press',
      bestEstimated1rmKg: 80,
      bodyweightKg: 80,
      sex: 'female',
    })!;
    expect(female).toBeGreaterThan(male);
  });
});

describe('nextExerciseRank — anti-inflation (PLAN.txt §6.6)', () => {
  it('requires two qualifying sessions to reach S', () => {
    expect(nextExerciseRank(null, 90, 1)).toBe('A'); // S capped to A with 1 session
    expect(nextExerciseRank(null, 90, 2)).toBe('S');
  });

  it('caps a single update to +2 rank bands', () => {
    // E (idx 0) with an S-level score can only rise to C (idx 2) this update.
    expect(nextExerciseRank('E', 90, 5)).toBe('C');
  });

  it('never decreases (permanent rank is a high-water mark)', () => {
    expect(nextExerciseRank('B', 5, 5)).toBe('B');
  });

  it('allows A -> S only with two sessions', () => {
    expect(nextExerciseRank('A', 85, 1)).toBe('A');
    expect(nextExerciseRank('A', 85, 2)).toBe('S');
  });
});

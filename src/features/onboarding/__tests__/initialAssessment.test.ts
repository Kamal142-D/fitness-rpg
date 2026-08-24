import {
  computeInitialAssessment,
  INITIAL_ASSESSMENT_VERSION,
} from '@/features/onboarding/initialAssessment';
import { emptyDraft, type OnboardingDraft } from '@/features/onboarding/types';

function draft(overrides: Partial<OnboardingDraft> = {}): OnboardingDraft {
  return { ...emptyDraft(), ...overrides };
}

describe('computeInitialAssessment', () => {
  it('produces clamped 0..100 attributes and a valid rank for an empty draft', () => {
    const a = computeInitialAssessment(draft());
    for (const v of [a.strength, a.physique, a.endurance, a.discipline, a.hunterScore]) {
      expect(v).toBeGreaterThanOrEqual(0);
      expect(v).toBeLessThanOrEqual(100);
    }
    expect(['E', 'D', 'C', 'B', 'A', 'S']).toContain(a.hunterRank);
    expect(a.version).toBe(INITIAL_ASSESSMENT_VERSION);
  });

  it('treats a null experience level as beginner', () => {
    const nullExp = computeInitialAssessment(draft({ experience_level: null }));
    const beginner = computeInitialAssessment(draft({ experience_level: 'beginner' }));
    expect(nullExp.strength).toBe(beginner.strength);
    expect(nullExp.physique).toBe(beginner.physique);
  });

  it('scores advanced higher than beginner, all else equal', () => {
    const base = { training_days_per_week: 4 } as const;
    const beginner = computeInitialAssessment(draft({ ...base, experience_level: 'beginner' }));
    const advanced = computeInitialAssessment(draft({ ...base, experience_level: 'advanced' }));
    expect(advanced.hunterScore).toBeGreaterThan(beginner.hunterScore);
  });

  it('uses relative strength when all three lifts + bodyweight are given', () => {
    const strong = computeInitialAssessment(
      draft({
        experience_level: 'beginner',
        current_weight_kg: 80,
        baseline_bench_kg: 120,
        baseline_squat_kg: 180,
        baseline_deadlift_kg: 220,
      }),
    );
    const noLifts = computeInitialAssessment(
      draft({ experience_level: 'beginner', current_weight_kg: 80 }),
    );
    expect(strong.strength).toBeGreaterThan(noLifts.strength);
    expect(strong.strength).toBeLessThanOrEqual(100);
  });

  it('does not use lift data unless all three lifts are present', () => {
    const partial = computeInitialAssessment(
      draft({ experience_level: 'beginner', current_weight_kg: 80, baseline_bench_kg: 120 }),
    );
    const none = computeInitialAssessment(
      draft({ experience_level: 'beginner', current_weight_kg: 80 }),
    );
    expect(partial.strength).toBe(none.strength);
  });

  it('does not extra-reward training 7 days over the 3-5 sweet spot (discipline)', () => {
    const five = computeInitialAssessment(draft({ training_days_per_week: 5 }));
    const seven = computeInitialAssessment(draft({ training_days_per_week: 7 }));
    expect(seven.discipline).toBeLessThanOrEqual(five.discipline);
  });

  it('rewards a healthy body-fat range over an extreme one (physique)', () => {
    const healthy = computeInitialAssessment(
      draft({ experience_level: 'intermediate', sex: 'male', body_fat_percent: 14 }),
    );
    const extreme = computeInitialAssessment(
      draft({ experience_level: 'intermediate', sex: 'male', body_fat_percent: 45 }),
    );
    expect(healthy.physique).toBeGreaterThan(extreme.physique);
  });
});

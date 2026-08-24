import { ageFromDob, isValidDateString, validateStep } from '@/features/onboarding/schemas';
import { emptyDraft, type OnboardingDraft } from '@/features/onboarding/types';

function draft(overrides: Partial<OnboardingDraft> = {}): OnboardingDraft {
  return { ...emptyDraft(), ...overrides };
}

describe('isValidDateString', () => {
  it('accepts a real date', () => {
    expect(isValidDateString('1998-05-20')).toBe(true);
  });
  it('rejects a bad format or impossible date', () => {
    expect(isValidDateString('1998-5-20')).toBe(false);
    expect(isValidDateString('2001-02-30')).toBe(false);
    expect(isValidDateString('not-a-date')).toBe(false);
  });
});

describe('ageFromDob', () => {
  it('computes whole-year age relative to a fixed now', () => {
    const now = new Date(Date.UTC(2026, 0, 1));
    expect(ageFromDob('2000-01-01', now)).toBe(26);
    expect(ageFromDob('2000-06-01', now)).toBe(25); // birthday not yet reached
  });
});

describe('validateStep: identity', () => {
  it('flags missing name, dob, and sex', () => {
    const e = validateStep('identity', draft());
    expect(e.display_name).toBeDefined();
    expect(e.date_of_birth).toBeDefined();
    expect(e.sex).toBeDefined();
  });
  it('passes a valid identity', () => {
    const e = validateStep(
      'identity',
      draft({ display_name: 'Kai', date_of_birth: '1998-05-20', sex: 'male' }),
    );
    expect(Object.keys(e)).toHaveLength(0);
  });
  it('rejects an out-of-range age', () => {
    const e = validateStep(
      'identity',
      draft({ display_name: 'Kai', date_of_birth: '2020-01-01', sex: 'male' }),
    );
    expect(e.date_of_birth).toBeDefined();
  });
});

describe('validateStep: measurements', () => {
  it('flags out-of-range height/weight', () => {
    const e = validateStep('measurements', draft({ height_cm: 50, current_weight_kg: 5 }));
    expect(e.height_cm).toBeDefined();
    expect(e.current_weight_kg).toBeDefined();
  });
  it('passes valid measurements', () => {
    const e = validateStep('measurements', draft({ height_cm: 178, current_weight_kg: 75 }));
    expect(Object.keys(e)).toHaveLength(0);
  });
});

describe('validateStep: schedule', () => {
  it('requires days, location, and a sane duration', () => {
    const e = validateStep('schedule', draft());
    expect(e.training_days_per_week).toBeDefined();
    expect(e.training_location).toBeDefined();
    expect(e.preferred_workout_minutes).toBeDefined();
  });
  it('passes a valid schedule', () => {
    const e = validateStep(
      'schedule',
      draft({ training_days_per_week: 4, training_location: 'gym', preferred_workout_minutes: 60 }),
    );
    expect(Object.keys(e)).toHaveLength(0);
  });
});

describe('validateStep: details (all optional)', () => {
  it('passes when everything is empty', () => {
    expect(Object.keys(validateStep('details', draft()))).toHaveLength(0);
  });
  it('validates only provided values', () => {
    const e = validateStep('details', draft({ body_fat_percent: 90, baseline_bench_kg: 9999 }));
    expect(e.body_fat_percent).toBeDefined();
    expect(e.baseline_bench_kg).toBeDefined();
    expect(e.skeletal_muscle_mass_kg).toBeUndefined();
  });
});

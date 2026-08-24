import type { OnboardingDraft } from '@/features/onboarding/types';

export type FieldErrors = Partial<Record<keyof OnboardingDraft, string>>;

export type StepId =
  | 'welcome'
  | 'identity'
  | 'measurements'
  | 'goal'
  | 'experience'
  | 'schedule'
  | 'details'
  | 'reveal';

/** Steps that require validation before advancing. */
export const VALIDATED_STEPS: readonly StepId[] = [
  'identity',
  'measurements',
  'goal',
  'experience',
  'schedule',
  'details',
];

/** True for a real calendar date in strict YYYY-MM-DD form. */
export function isValidDateString(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [y, m, d] = value.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  return date.getUTCFullYear() === y && date.getUTCMonth() === m - 1 && date.getUTCDate() === d;
}

/** Whole-year age from a YYYY-MM-DD date of birth, relative to `now`. */
export function ageFromDob(dob: string, now: Date = new Date()): number {
  const [y, m, d] = dob.split('-').map(Number);
  let age = now.getUTCFullYear() - y;
  const beforeBirthday =
    now.getUTCMonth() + 1 < m || (now.getUTCMonth() + 1 === m && now.getUTCDate() < d);
  if (beforeBirthday) age -= 1;
  return age;
}

function inRange(v: number | null, lo: number, hi: number): boolean {
  return v != null && v >= lo && v <= hi;
}

/** Validate the fields owned by a given step. Returns per-field messages. */
export function validateStep(step: StepId, d: OnboardingDraft): FieldErrors {
  const e: FieldErrors = {};
  switch (step) {
    case 'identity': {
      if (!d.display_name.trim()) e.display_name = 'Enter a name';
      else if (d.display_name.trim().length > 40) e.display_name = 'Keep it under 40 characters';
      if (!d.date_of_birth) e.date_of_birth = 'Enter your date of birth';
      else if (!isValidDateString(d.date_of_birth)) e.date_of_birth = 'Use the format YYYY-MM-DD';
      else {
        const age = ageFromDob(d.date_of_birth);
        if (age < 13 || age > 100) e.date_of_birth = 'Age must be between 13 and 100';
      }
      if (!d.sex) e.sex = 'Select an option';
      break;
    }
    case 'measurements': {
      if (!inRange(d.height_cm, 100, 250)) e.height_cm = 'Enter a height between 100 and 250 cm';
      if (!inRange(d.current_weight_kg, 30, 300))
        e.current_weight_kg = 'Enter a weight between 30 and 300 kg';
      break;
    }
    case 'goal': {
      if (!d.fitness_goal) e.fitness_goal = 'Choose a goal';
      break;
    }
    case 'experience': {
      if (!d.experience_level) e.experience_level = 'Choose your experience level';
      break;
    }
    case 'schedule': {
      if (!inRange(d.training_days_per_week, 1, 7))
        e.training_days_per_week = 'Choose how many days you can train';
      if (!d.training_location) e.training_location = 'Choose where you train';
      if (!inRange(d.preferred_workout_minutes, 10, 240))
        e.preferred_workout_minutes = 'Enter a duration between 10 and 240 minutes';
      break;
    }
    case 'details': {
      // All optional; validate only when a value is present.
      if (d.body_fat_percent != null && !inRange(d.body_fat_percent, 3, 60))
        e.body_fat_percent = 'Body fat should be between 3 and 60%';
      if (d.skeletal_muscle_mass_kg != null && !inRange(d.skeletal_muscle_mass_kg, 10, 80))
        e.skeletal_muscle_mass_kg = 'Muscle mass should be between 10 and 80 kg';
      for (const k of ['baseline_bench_kg', 'baseline_squat_kg', 'baseline_deadlift_kg'] as const) {
        const v = d[k];
        if (v != null && !inRange(v, 1, 500)) e[k] = 'Enter a value between 1 and 500 kg';
      }
      break;
    }
    default:
      break;
  }
  return e;
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.keys(errors).length > 0;
}

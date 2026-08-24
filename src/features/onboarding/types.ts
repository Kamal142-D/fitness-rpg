import type { ChoiceOption } from '@/components/ui';

export type Sex = 'male' | 'female' | 'intersex' | 'prefer_not_to_say';
export type ExperienceLevel = 'beginner' | 'intermediate' | 'advanced';
export type FitnessGoal =
  'build_muscle' | 'lose_fat' | 'get_stronger' | 'general_fitness' | 'improve_endurance';
export type TrainingLocation = 'gym' | 'home';

/**
 * The onboarding draft: everything collected across the Awakening steps before
 * it is persisted. Numeric fields are null until entered. Optional body-comp and
 * baseline-lift fields refine the initial assessment but are not required.
 */
export interface OnboardingDraft {
  display_name: string;
  date_of_birth: string; // YYYY-MM-DD
  sex: Sex | null;
  height_cm: number | null;
  current_weight_kg: number | null;
  fitness_goal: FitnessGoal | null;
  experience_level: ExperienceLevel | null;
  training_days_per_week: number | null;
  training_location: TrainingLocation | null;
  preferred_workout_minutes: number | null;
  // Optional details
  body_fat_percent: number | null;
  skeletal_muscle_mass_kg: number | null;
  baseline_bench_kg: number | null;
  baseline_squat_kg: number | null;
  baseline_deadlift_kg: number | null;
}

export const SEX_OPTIONS: readonly ChoiceOption<Sex>[] = [
  { label: 'Male', value: 'male' },
  { label: 'Female', value: 'female' },
  { label: 'Intersex', value: 'intersex' },
  { label: 'Prefer not to say', value: 'prefer_not_to_say' },
];

export const EXPERIENCE_OPTIONS: readonly ChoiceOption<ExperienceLevel>[] = [
  { label: 'Beginner', value: 'beginner', description: 'New to training, or under ~6 months' },
  {
    label: 'Intermediate',
    value: 'intermediate',
    description: 'Consistent for 6 months to a few years',
  },
  { label: 'Advanced', value: 'advanced', description: 'Years of consistent, structured training' },
];

export const GOAL_OPTIONS: readonly ChoiceOption<FitnessGoal>[] = [
  { label: 'Build muscle', value: 'build_muscle' },
  { label: 'Lose fat', value: 'lose_fat' },
  { label: 'Get stronger', value: 'get_stronger' },
  { label: 'General fitness', value: 'general_fitness' },
  { label: 'Improve endurance', value: 'improve_endurance' },
];

export const LOCATION_OPTIONS: readonly ChoiceOption<TrainingLocation>[] = [
  { label: 'Gym', value: 'gym', description: 'Full equipment access' },
  { label: 'Home', value: 'home', description: 'Bodyweight or limited equipment' },
];

export const TRAINING_DAYS_OPTIONS: readonly ChoiceOption<string>[] = [
  { label: '1 day', value: '1' },
  { label: '2 days', value: '2' },
  { label: '3 days', value: '3' },
  { label: '4 days', value: '4' },
  { label: '5 days', value: '5' },
  { label: '6 days', value: '6' },
  { label: '7 days', value: '7' },
];

/** A fresh, empty draft. */
export function emptyDraft(): OnboardingDraft {
  return {
    display_name: '',
    date_of_birth: '',
    sex: null,
    height_cm: null,
    current_weight_kg: null,
    fitness_goal: null,
    experience_level: null,
    training_days_per_week: null,
    training_location: null,
    preferred_workout_minutes: null,
    body_fat_percent: null,
    skeletal_muscle_mass_kg: null,
    baseline_bench_kg: null,
    baseline_squat_kg: null,
    baseline_deadlift_kg: null,
  };
}

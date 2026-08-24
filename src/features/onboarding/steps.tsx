import { View } from 'react-native';

import { ChoiceGroup, Text, TextField } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { NumberField } from '@/features/onboarding/steps/NumberField';
import type { FieldErrors, StepId } from '@/features/onboarding/schemas';
import { useOnboardingStore } from '@/features/onboarding/useOnboardingStore';
import {
  EXPERIENCE_OPTIONS,
  GOAL_OPTIONS,
  LOCATION_OPTIONS,
  SEX_OPTIONS,
  TRAINING_DAYS_OPTIONS,
} from '@/features/onboarding/types';

export interface StepProps {
  errors: FieldErrors;
}

function StepHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <View style={{ gap: Spacing.xs }}>
      <Text variant="title">{title}</Text>
      {subtitle ? (
        <Text variant="body" color="secondary">
          {subtitle}
        </Text>
      ) : null}
    </View>
  );
}

export function WelcomeStep() {
  return (
    <View style={{ gap: Spacing.lg, marginTop: Spacing.xl }}>
      <Text variant="caption" color="secondary">
        SYSTEM INITIALIZING
      </Text>
      <Text variant="display">The Awakening</Text>
      <Text variant="body" color="secondary">
        Answer a few questions so the System can read your starting condition and assign your first
        rank. This takes about a minute. Your real training will do the rest.
      </Text>
    </View>
  );
}

export function IdentityStep({ errors }: StepProps) {
  const { draft, set } = useOnboardingStore();
  return (
    <View style={{ gap: Spacing.lg }}>
      <StepHeader title="Who are you?" subtitle="The basics for your Hunter profile." />
      <TextField
        label="Display name"
        placeholder="e.g. Kai"
        value={draft.display_name}
        onChangeText={(t) => set('display_name', t)}
        error={errors.display_name}
        autoCapitalize="words"
      />
      <TextField
        label="Date of birth"
        placeholder="YYYY-MM-DD"
        value={draft.date_of_birth}
        onChangeText={(t) => set('date_of_birth', t)}
        error={errors.date_of_birth}
        keyboardType="numbers-and-punctuation"
        autoCapitalize="none"
      />
      <View style={{ gap: Spacing.sm }}>
        <Text variant="caption" color="secondary" style={{ marginLeft: Spacing.xs }}>
          SEX
        </Text>
        <ChoiceGroup options={SEX_OPTIONS} value={draft.sex} onChange={(v) => set('sex', v)} />
        {errors.sex ? (
          <Text variant="caption" color="danger" style={{ marginLeft: Spacing.xs }}>
            {errors.sex}
          </Text>
        ) : null}
      </View>
    </View>
  );
}

export function MeasurementsStep({ errors }: StepProps) {
  const { draft, set } = useOnboardingStore();
  return (
    <View style={{ gap: Spacing.lg }}>
      <StepHeader title="Measurements" subtitle="Stored in metric. Used to scale your stats." />
      <NumberField
        label="Height (cm)"
        placeholder="e.g. 178"
        value={draft.height_cm}
        onChange={(v) => set('height_cm', v)}
        error={errors.height_cm}
      />
      <NumberField
        label="Weight (kg)"
        placeholder="e.g. 75"
        value={draft.current_weight_kg}
        onChange={(v) => set('current_weight_kg', v)}
        error={errors.current_weight_kg}
      />
    </View>
  );
}

export function GoalStep({ errors }: StepProps) {
  const { draft, set } = useOnboardingStore();
  return (
    <View style={{ gap: Spacing.lg }}>
      <StepHeader title="Your goal" subtitle="What are you training for right now?" />
      <ChoiceGroup
        options={GOAL_OPTIONS}
        value={draft.fitness_goal}
        onChange={(v) => set('fitness_goal', v)}
      />
      {errors.fitness_goal ? (
        <Text variant="caption" color="danger">
          {errors.fitness_goal}
        </Text>
      ) : null}
    </View>
  );
}

export function ExperienceStep({ errors }: StepProps) {
  const { draft, set } = useOnboardingStore();
  return (
    <View style={{ gap: Spacing.lg }}>
      <StepHeader title="Experience" subtitle="How long have you trained consistently?" />
      <ChoiceGroup
        options={EXPERIENCE_OPTIONS}
        value={draft.experience_level}
        onChange={(v) => set('experience_level', v)}
      />
      {errors.experience_level ? (
        <Text variant="caption" color="danger">
          {errors.experience_level}
        </Text>
      ) : null}
    </View>
  );
}

export function ScheduleStep({ errors }: StepProps) {
  const { draft, set } = useOnboardingStore();
  return (
    <View style={{ gap: Spacing.lg }}>
      <StepHeader title="Your schedule" subtitle="Rest days are healthy. Be realistic." />
      <View style={{ gap: Spacing.sm }}>
        <Text variant="caption" color="secondary" style={{ marginLeft: Spacing.xs }}>
          DAYS PER WEEK
        </Text>
        <ChoiceGroup
          options={TRAINING_DAYS_OPTIONS}
          value={draft.training_days_per_week == null ? null : String(draft.training_days_per_week)}
          onChange={(v) => set('training_days_per_week', Number(v))}
        />
        {errors.training_days_per_week ? (
          <Text variant="caption" color="danger" style={{ marginLeft: Spacing.xs }}>
            {errors.training_days_per_week}
          </Text>
        ) : null}
      </View>
      <View style={{ gap: Spacing.sm }}>
        <Text variant="caption" color="secondary" style={{ marginLeft: Spacing.xs }}>
          WHERE
        </Text>
        <ChoiceGroup
          options={LOCATION_OPTIONS}
          value={draft.training_location}
          onChange={(v) => set('training_location', v)}
        />
        {errors.training_location ? (
          <Text variant="caption" color="danger" style={{ marginLeft: Spacing.xs }}>
            {errors.training_location}
          </Text>
        ) : null}
      </View>
      <NumberField
        label="Preferred session length (minutes)"
        placeholder="e.g. 60"
        value={draft.preferred_workout_minutes}
        onChange={(v) => set('preferred_workout_minutes', v)}
        error={errors.preferred_workout_minutes}
        decimal={false}
      />
    </View>
  );
}

export function DetailsStep({ errors }: StepProps) {
  const { draft, set } = useOnboardingStore();
  return (
    <View style={{ gap: Spacing.lg }}>
      <StepHeader
        title="Optional details"
        subtitle="Skip anything you don't know. These sharpen your starting rank."
      />
      <NumberField
        label="Body fat %"
        placeholder="Optional"
        value={draft.body_fat_percent}
        onChange={(v) => set('body_fat_percent', v)}
        error={errors.body_fat_percent}
      />
      <NumberField
        label="Skeletal muscle mass (kg)"
        placeholder="Optional"
        value={draft.skeletal_muscle_mass_kg}
        onChange={(v) => set('skeletal_muscle_mass_kg', v)}
        error={errors.skeletal_muscle_mass_kg}
      />
      <Text variant="caption" color="secondary" style={{ marginTop: Spacing.sm }}>
        CURRENT BEST LIFTS (1 REP MAX, OPTIONAL)
      </Text>
      <NumberField
        label="Bench press (kg)"
        placeholder="Optional"
        value={draft.baseline_bench_kg}
        onChange={(v) => set('baseline_bench_kg', v)}
        error={errors.baseline_bench_kg}
      />
      <NumberField
        label="Squat (kg)"
        placeholder="Optional"
        value={draft.baseline_squat_kg}
        onChange={(v) => set('baseline_squat_kg', v)}
        error={errors.baseline_squat_kg}
      />
      <NumberField
        label="Deadlift (kg)"
        placeholder="Optional"
        value={draft.baseline_deadlift_kg}
        onChange={(v) => set('baseline_deadlift_kg', v)}
        error={errors.baseline_deadlift_kg}
      />
    </View>
  );
}

export interface StepConfig {
  id: StepId;
  Component: (props: StepProps) => React.JSX.Element;
}

/** Ordered wizard steps (the rank reveal is handled by the screen after persist). */
export const STEPS: readonly StepConfig[] = [
  { id: 'welcome', Component: WelcomeStep },
  { id: 'identity', Component: IdentityStep },
  { id: 'measurements', Component: MeasurementsStep },
  { id: 'goal', Component: GoalStep },
  { id: 'experience', Component: ExperienceStep },
  { id: 'schedule', Component: ScheduleStep },
  { id: 'details', Component: DetailsStep },
];

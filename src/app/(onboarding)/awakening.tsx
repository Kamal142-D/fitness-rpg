import { useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import { Splash } from '@/components/Splash';
import { Button, ProgressBar, Screen, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/features/auth';
import {
  completeOnboarding,
  computeInitialAssessment,
  hasErrors,
  RankReveal,
  STEPS,
  useOnboardingStore,
  validateStep,
  type FieldErrors,
  type InitialAssessment,
} from '@/features/onboarding';

export default function OnboardingScreen() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { draft, reset } = useOnboardingStore();

  const [stepIndex, setStepIndex] = useState(0);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [assessment, setAssessment] = useState<InitialAssessment | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  if (!user) return <Splash />;

  const step = STEPS[stepIndex];
  const isFirst = stepIndex === 0;
  const isLastQuestion = stepIndex === STEPS.length - 1;
  const totalStops = STEPS.length + 1; // + rank reveal
  const progress = assessment ? 1 : (stepIndex + 1) / totalStops;

  function onNext() {
    const stepErrors = validateStep(step.id, draft);
    if (hasErrors(stepErrors)) {
      setErrors(stepErrors);
      return;
    }
    setErrors({});
    if (isLastQuestion) {
      setAssessment(computeInitialAssessment(draft));
    } else {
      setStepIndex((i) => i + 1);
    }
  }

  function onBack() {
    setErrors({});
    setStepIndex((i) => Math.max(0, i - 1));
  }

  async function onFinish() {
    if (!assessment || !user) return;
    setSaving(true);
    setSaveError(null);
    const result = await completeOnboarding(user.id, draft, assessment);
    if (!result.ok) {
      setSaveError(result.message);
      setSaving(false);
      return;
    }
    await queryClient.invalidateQueries({ queryKey: ['profile', user.id] });
    reset();
    router.replace('/system');
  }

  // Rank reveal phase.
  if (assessment) {
    return (
      <Screen>
        <ProgressBar value={1} />
        <RankReveal
          assessment={assessment}
          name={draft.display_name}
          onContinue={onFinish}
          saving={saving}
          error={saveError}
        />
      </Screen>
    );
  }

  const StepComponent = step.Component;
  return (
    <Screen>
      <View style={{ gap: Spacing.sm }}>
        <ProgressBar value={progress} />
        <Text variant="caption" color="tertiary">
          STEP {stepIndex + 1} OF {STEPS.length}
        </Text>
      </View>

      <StepComponent errors={errors} />

      <View style={{ flexDirection: 'row', gap: Spacing.md, marginTop: Spacing.md }}>
        {!isFirst ? (
          <Button label="Back" variant="secondary" onPress={onBack} style={{ flex: 1 }} />
        ) : null}
        <Button
          label={isFirst ? 'Begin' : isLastQuestion ? 'Reveal my rank' : 'Continue'}
          onPress={onNext}
          style={{ flex: 2 }}
        />
      </View>
    </Screen>
  );
}

export { useOnboardingStore } from '@/features/onboarding/useOnboardingStore';
export {
  computeInitialAssessment,
  INITIAL_ASSESSMENT_VERSION,
} from '@/features/onboarding/initialAssessment';
export type { InitialAssessment } from '@/features/onboarding/initialAssessment';
export { completeOnboarding, getProfile } from '@/features/onboarding/api';
export type { Profile, PersistResult } from '@/features/onboarding/api';
export { useProfile } from '@/features/onboarding/useProfile';
export { validateStep, hasErrors, VALIDATED_STEPS } from '@/features/onboarding/schemas';
export type { FieldErrors, StepId } from '@/features/onboarding/schemas';
export { STEPS } from '@/features/onboarding/steps';
export type { StepProps, StepConfig } from '@/features/onboarding/steps';
export { RankReveal } from '@/features/onboarding/RankReveal';
export * from '@/features/onboarding/types';

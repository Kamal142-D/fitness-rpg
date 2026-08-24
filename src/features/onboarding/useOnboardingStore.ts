import { create } from 'zustand';

import { emptyDraft, type OnboardingDraft } from '@/features/onboarding/types';

interface OnboardingState {
  draft: OnboardingDraft;
  /** Set a single draft field. */
  set: <K extends keyof OnboardingDraft>(key: K, value: OnboardingDraft[K]) => void;
  /** Merge several fields at once. */
  patch: (partial: Partial<OnboardingDraft>) => void;
  reset: () => void;
}

/**
 * Holds the in-progress Awakening answers across steps. Small and focused (the
 * intended Zustand pattern) — it is discarded once onboarding is persisted.
 */
export const useOnboardingStore = create<OnboardingState>((set) => ({
  draft: emptyDraft(),
  set: (key, value) => set((s) => ({ draft: { ...s.draft, [key]: value } })),
  patch: (partial) => set((s) => ({ draft: { ...s.draft, ...partial } })),
  reset: () => set({ draft: emptyDraft() }),
}));

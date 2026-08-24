import { create } from 'zustand';

/**
 * Example of the intended client-state pattern: a small, focused Zustand store
 * for local UI preferences only.
 *
 * Deliberately NOT a monolithic global store, and NOT a mirror of remote data
 * (that is TanStack Query's job). Real feature stores (e.g. an active-workout
 * store in Phase 7) follow this same small-and-focused shape.
 */
interface PreferencesState {
  /** Whether the user has dismissed the first-run foundation notice. */
  hasSeenIntro: boolean;
  markIntroSeen: () => void;
  reset: () => void;
}

export const usePreferencesStore = create<PreferencesState>((set) => ({
  hasSeenIntro: false,
  markIntroSeen: () => set({ hasSeenIntro: true }),
  reset: () => set({ hasSeenIntro: false }),
}));

import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

import type { GateDetail } from '@/features/gates/types';
import * as L from '@/features/workouts/logic';
import type { ActiveSet, ActiveWorkout } from '@/features/workouts/types';

interface ActiveWorkoutState {
  workout: ActiveWorkout | null;
  start: (detail: GateDetail) => void;
  completeSet: (exIdx: number, setIdx: number) => void;
  uncompleteSet: (exIdx: number, setIdx: number) => void;
  updateSet: (
    exIdx: number,
    setIdx: number,
    patch: Partial<Pick<ActiveSet, 'weightKg' | 'reps' | 'rpe'>>,
  ) => void;
  toggleWarmup: (exIdx: number, setIdx: number) => void;
  addSet: (exIdx: number) => void;
  removeSet: (exIdx: number, setIdx: number) => void;
  goToExercise: (idx: number) => void;
  setNotes: (exIdx: number, notes: string) => void;
  clearRest: () => void;
  addRest: (seconds: number) => void;
  abandon: () => void;
}

/**
 * The active workout, persisted to AsyncStorage so it autosaves on every change
 * and survives app restarts (resume). All mutations delegate to the pure
 * reducers in logic.ts; this module only wires persistence + React.
 */
export const useActiveWorkoutStore = create<ActiveWorkoutState>()(
  persist(
    (set) => {
      const apply = (fn: (w: ActiveWorkout) => ActiveWorkout) =>
        set((s) => (s.workout ? { workout: fn(s.workout) } : s));
      return {
        workout: null,
        start: (detail) => set({ workout: L.createActiveWorkout(detail) }),
        completeSet: (e, i) => apply((w) => L.completeSet(w, e, i)),
        uncompleteSet: (e, i) => apply((w) => L.uncompleteSet(w, e, i)),
        updateSet: (e, i, patch) => apply((w) => L.updateSet(w, e, i, patch)),
        toggleWarmup: (e, i) => apply((w) => L.toggleWarmup(w, e, i)),
        addSet: (e) => apply((w) => L.addSet(w, e)),
        removeSet: (e, i) => apply((w) => L.removeSet(w, e, i)),
        goToExercise: (idx) => apply((w) => L.setCurrentExercise(w, idx)),
        setNotes: (e, notes) => apply((w) => L.setNotes(w, e, notes)),
        clearRest: () => apply((w) => L.clearRest(w)),
        addRest: (seconds) => apply((w) => L.addRestSeconds(w, seconds)),
        abandon: () => set({ workout: null }),
      };
    },
    {
      name: 'active-workout',
      version: 1,
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (s) => ({ workout: s.workout }),
    },
  ),
);

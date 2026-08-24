import type { Rank } from '@/constants/ranks';

export interface ActiveSet {
  id: string;
  setNumber: number;
  weightKg: number | null;
  reps: number | null;
  rpe: number | null;
  isWarmup: boolean;
  isCompleted: boolean;
  completedAt: string | null;
}

export interface ActiveExercise {
  id: string;
  exerciseId: string;
  name: string;
  primaryMuscle: string | null;
  targetSets: number | null;
  targetRepsMin: number | null;
  targetRepsMax: number | null;
  targetRpe: number | null;
  restSeconds: number | null;
  rankingEnabled: boolean;
  notes: string;
  sets: ActiveSet[];
}

export interface ActiveWorkout {
  /** Client-generated session id (idempotency key for completion). */
  sessionId: string;
  templateId: string | null;
  name: string;
  gateDifficulty: Rank | null;
  startedAt: string;
  currentExerciseIndex: number;
  /** Epoch ms when the current rest ends, or null when not resting. */
  restEndsAt: number | null;
  exercises: ActiveExercise[];
}

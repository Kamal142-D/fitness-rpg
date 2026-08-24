export type RecordType = 'weight' | 'reps' | 'estimated_1rm' | 'volume';

/** A user's prior all-time bests for one exercise (from exercise_user_stats). */
export interface PriorStat {
  bestWeightKg: number | null;
  bestReps: number | null;
  bestEstimated1rmKg: number | null;
  bestVolumeKg: number | null;
}

export interface DetectSet {
  setNumber: number;
  weightKg: number | null;
  reps: number | null;
  est1RM: number | null;
  isWarmup: boolean;
}

export interface DetectExercise {
  exerciseId: string;
  orderIndex: number;
  sets: DetectSet[];
}

export interface DetectedPR {
  exerciseId: string;
  orderIndex: number;
  /** Set that achieved the record (for linking workout_set_id server-side). */
  setNumber: number;
  recordType: RecordType;
  previousValue: number | null;
  newValue: number;
}

/** All-time bests for an exercise after this workout (for exercise_user_stats). */
export interface NewStat {
  exerciseId: string;
  bestWeightKg: number | null;
  bestReps: number | null;
  bestEstimated1rmKg: number | null;
  bestVolumeKg: number | null;
}

export interface DetectResult {
  prs: DetectedPR[];
  stats: NewStat[];
}

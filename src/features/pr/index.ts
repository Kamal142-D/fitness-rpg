export type {
  RecordType,
  PriorStat,
  DetectSet,
  DetectExercise,
  DetectedPR,
  NewStat,
  DetectResult,
} from '@/features/pr/types';
export { detectPRs, prioritizePRs } from '@/features/pr/detect';
export { getExerciseStats, applyWorkoutResults } from '@/features/pr/api';
export { useFinishWorkout } from '@/features/pr/useFinishWorkout';
export type { FinishWorkoutInput, FinishWorkoutResult } from '@/features/pr/useFinishWorkout';

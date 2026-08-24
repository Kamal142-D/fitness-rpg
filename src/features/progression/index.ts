export { getXpRequiredForLevel, xpProgress } from '@/features/progression/xp';
export type { XpProgress } from '@/features/progression/xp';
export { xpForWorkout, applyXp, XP_REWARDS } from '@/features/progression/rewards';
export type {
  WorkoutXpInput,
  ProgressionSnapshot,
  XpApplication,
} from '@/features/progression/rewards';
export {
  getProgression,
  applySessionProgression,
  getFinishInputs,
} from '@/features/progression/api';
export type { Progression } from '@/features/progression/api';
export { buildProgressionUpdate, computeAttributes } from '@/features/progression/finalize';
export type {
  AttributeInputs,
  FinishInputs,
  ProgressionUpdateInput,
  ProgressionPersistPayload,
} from '@/features/progression/finalize';
export { useProgression } from '@/features/progression/useProgression';

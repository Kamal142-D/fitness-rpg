/**
 * Anti-inflation validation (PLAN.txt §6.6). Pure. Raw logs are never modified —
 * these helpers only decide which sets QUALIFY for ranking and explain why.
 */
import { VALIDATION_LIMITS as L } from '@/services/ranking/config';

export interface RankingSetInput {
  weightKg: number | null;
  reps: number | null;
  rpe: number | null;
  isWarmup: boolean;
  isCompleted: boolean;
}

export interface SetValidation {
  valid: boolean;
  /** Why the set was excluded, for user-facing explanation. Null when valid. */
  reason: string | null;
}

/**
 * A set qualifies for strength ranking when it is a completed working set with
 * plausible, loaded weight and reps in the strength range (1..12). Warm-ups,
 * bodyweight-only (no load), and implausible values are excluded.
 */
export function validateWorkingSet(set: RankingSetInput): SetValidation {
  if (!set.isCompleted) return { valid: false, reason: 'Set was not completed' };
  if (set.isWarmup) return { valid: false, reason: 'Warm-up sets do not count' };

  if (set.weightKg == null || set.weightKg <= L.minWeightKg) {
    return { valid: false, reason: 'No load recorded' };
  }
  if (set.weightKg > L.maxWeightKg) {
    return { valid: false, reason: 'Weight is implausibly high' };
  }
  if (set.reps == null || set.reps < L.minReps) {
    return { valid: false, reason: 'No reps recorded' };
  }
  if (set.reps > L.maxReps) {
    return { valid: false, reason: 'Reps are implausibly high' };
  }
  if (set.reps > 12) {
    return { valid: false, reason: 'Reps above 12 are not used for strength ranking' };
  }
  if (set.rpe != null && (set.rpe < L.minRpe || set.rpe > L.maxRpe)) {
    return { valid: false, reason: 'RPE is out of range' };
  }
  return { valid: true, reason: null };
}

/** The subset of sets that qualify for ranking. */
export function qualifyingWorkingSets<T extends RankingSetInput>(sets: T[]): T[] {
  return sets.filter((s) => validateWorkingSet(s).valid);
}

/**
 * Whether a set of qualifying sets is enough to rank a performance at all
 * (PLAN.txt §6.6: don't rank from one implausible set — need ≥2 valid sets).
 */
export function meetsQualifyingThreshold(qualifyingCount: number): boolean {
  return qualifyingCount >= L.minQualifyingSets;
}

/**
 * Permanent Exercise Rank (PLAN.txt §6.3) + anti-inflation upgrade guard (§6.6).
 *
 * Pipeline: best valid estimated-1RM -> bodyweight-relative ratio -> provisional
 * reference comparison -> normalized 0..100 score -> Exercise Rank.
 */
import { RANKS, clampScore, scoreToRank, type Rank } from '@/constants/ranks';
import {
  VALIDATION_LIMITS as L,
  movementForExercise,
  strengthAnchors,
  type Sex,
} from '@/services/ranking/config';
import { interpolate } from '@/services/ranking/interp';

export interface ExerciseScoreInput {
  exerciseName: string;
  bestEstimated1rmKg: number | null;
  bodyweightKg: number | null;
  sex: Sex;
}

/**
 * Normalized 0..100 capability score for an exercise, or null when it can't be
 * scored (no strength standard for the movement, or missing bodyweight / 1RM).
 */
export function exerciseScore(input: ExerciseScoreInput): number | null {
  const movement = movementForExercise(input.exerciseName);
  if (!movement) return null;
  if (
    input.bestEstimated1rmKg == null ||
    input.bestEstimated1rmKg <= 0 ||
    input.bodyweightKg == null ||
    input.bodyweightKg <= 0
  ) {
    return null;
  }
  const ratio = input.bestEstimated1rmKg / input.bodyweightKg;
  return clampScore(interpolate(strengthAnchors(movement, input.sex), ratio));
}

export function permanentExerciseRank(score: number): Rank {
  return scoreToRank(score);
}

/**
 * Apply the new capability score to a prior rank with anti-inflation rules:
 * - Permanent rank is a high-water mark (never decreases on a worse session).
 * - A single update may not jump more than `maxRankJump` bands (abnormal jumps
 *   are not auto-accepted).
 * - Reaching S requires `minSessionsForS` qualifying sessions.
 */
export function nextExerciseRank(
  prev: Rank | null,
  candidateScore: number,
  qualifyingSessions: number,
): Rank {
  const prevIdx = prev == null ? -1 : RANKS.indexOf(prev);
  let candIdx = RANKS.indexOf(scoreToRank(candidateScore));

  if (prevIdx >= 0) {
    candIdx = Math.min(candIdx, prevIdx + L.maxRankJump);
  }
  if (RANKS[candIdx] === 'S' && qualifyingSessions < L.minSessionsForS) {
    candIdx = RANKS.indexOf('A');
  }
  return RANKS[Math.max(prevIdx, candIdx)];
}

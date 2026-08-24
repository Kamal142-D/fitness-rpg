/**
 * Gate Score -> Gate Clear Rank (PLAN.txt §6.5). Computed AFTER a workout; the
 * post-training measure of session quality (never confused with Gate Difficulty,
 * which is chosen before). Every component and the result are clamped 0..100.
 *
 * Missing factors (e.g. no history for progress) are renormalized out rather than
 * scored as zero, so new users are not punished for absent comparison data.
 */
import { clampScore, scoreToRank, type Rank } from '@/constants/ranks';
import { GATE_WEIGHTS, NEUTRAL_SCORE } from '@/services/ranking/config';
import { ratioToScore } from '@/services/ranking/performanceGrade';

interface WeightedComponent {
  value: number | null;
  weight: number;
}

/** Weighted average over the non-null components, renormalizing their weights. */
export function weightedRenormalized(components: WeightedComponent[]): number {
  let sumW = 0;
  let sum = 0;
  for (const c of components) {
    if (c.value == null) continue;
    sumW += c.weight;
    sum += clampScore(c.value) * c.weight;
  }
  if (sumW === 0) return NEUTRAL_SCORE;
  return clampScore(sum / sumW);
}

export interface GateScoreInput {
  /** Avg performance grade score across exercises (0..100). */
  performance: number | null;
  /** Completed working sets / planned sets, as 0..100. */
  completion: number | null;
  /** Progress vs prior sessions (0..100), or null when no history. */
  progress: number | null;
  /** PR bonus (0..100). */
  pr: number | null;
  /** Training quality / RPE validity (0..100). */
  quality: number | null;
}

export function computeGateScore(input: GateScoreInput): number {
  return weightedRenormalized([
    { value: input.performance, weight: GATE_WEIGHTS.performance },
    { value: input.completion, weight: GATE_WEIGHTS.completion },
    { value: input.progress, weight: GATE_WEIGHTS.progress },
    { value: input.pr, weight: GATE_WEIGHTS.pr },
    { value: input.quality, weight: GATE_WEIGHTS.quality },
  ]);
}

export function gateClearRank(score: number): Rank {
  return scoreToRank(score);
}

/** Completion as a percentage of planned working sets. */
export function completionScore(completedWorkingSets: number, plannedSets: number): number {
  if (plannedSets <= 0) return completedWorkingSets > 0 ? 100 : 0;
  return clampScore((completedWorkingSets / plannedSets) * 100);
}

/** Progress vs a prior session's volume. Null when there is no prior history. */
export function progressScore(
  currentVolumeKg: number,
  priorVolumeKg: number | null,
): number | null {
  if (priorVolumeKg == null || priorVolumeKg <= 0) return null;
  return ratioToScore(currentVolumeKg / priorVolumeKg);
}

/** PR bonus: absent PRs are neutral (bonus, not a penalty). */
export function prComponentScore(prCount: number): number {
  if (prCount <= 0) return 50;
  if (prCount === 1) return 72;
  if (prCount === 2) return 86;
  return 100;
}

/**
 * Quality from RPE validity: the fraction of working sets carrying a plausible
 * RPE. No RPE logged at all is neutral (not a penalty).
 */
export function qualityScore(rpes: (number | null)[]): number {
  if (rpes.length === 0) return NEUTRAL_SCORE;
  const present = rpes.filter((r) => r != null) as number[];
  if (present.length === 0) return NEUTRAL_SCORE;
  const valid = present.filter((r) => r >= 0 && r <= 10).length;
  return clampScore((valid / present.length) * 100);
}

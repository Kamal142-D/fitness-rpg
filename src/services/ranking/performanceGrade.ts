/**
 * Workout Performance Grade (PLAN.txt §6.4): how the user performed TODAY versus
 * their expected recent performance. Distinct from permanent Exercise Rank.
 */
import { scoreToRank, clampScore, type Rank } from '@/constants/ranks';
import { NEUTRAL_SCORE } from '@/services/ranking/config';
import { interpolate, type Anchor } from '@/services/ranking/interp';

/** today/baseline ratio -> 0..100 performance score. PROVISIONAL. */
const RATIO_ANCHORS: readonly Anchor[] = [
  { x: 0.8, y: 20 },
  { x: 0.9, y: 40 },
  { x: 0.95, y: 52 },
  { x: 1.0, y: 65 },
  { x: 1.05, y: 78 },
  { x: 1.1, y: 88 },
  { x: 1.2, y: 100 },
];

/**
 * Score today's effort against a recent baseline. Missing baseline (first time)
 * or no valid effort today yields the neutral score — new users aren't punished.
 */
export function performanceScore(todayBest: number | null, baseline: number | null): number {
  if (todayBest == null || todayBest <= 0) return NEUTRAL_SCORE;
  if (baseline == null || baseline <= 0) return NEUTRAL_SCORE;
  return clampScore(interpolate(RATIO_ANCHORS, todayBest / baseline));
}

export function performanceGrade(score: number): Rank {
  return scoreToRank(score);
}

/** Exposed for reuse by progress scoring (same ratio curve). */
export function ratioToScore(ratio: number): number {
  return clampScore(interpolate(RATIO_ANCHORS, ratio));
}

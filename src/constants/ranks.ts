/**
 * Shared rank thresholds and pure rank-mapping helpers (PLAN.txt §6.1, §7).
 *
 * These are the ONE canonical definition of the E..S rank scale. The full
 * ranking ENGINE (exercise ranks, performance grades, gate scores, Hunter rank)
 * arrives in later phases and must reuse these thresholds — do not duplicate the
 * bands elsewhere. Keeping them here means they can be tuned without touching UI
 * or engine code.
 *
 * Everything in this file is pure and deterministic so it can be unit-tested in
 * isolation.
 */

import { Palette } from '@/constants/theme';

/** The rank ladder, weakest to strongest. */
export const RANKS = ['E', 'D', 'C', 'B', 'A', 'S'] as const;

export type Rank = (typeof RANKS)[number];

/**
 * Inclusive lower/upper score bounds for each rank on a 0..100 scale
 * (PLAN.txt §6.1). Ordered weakest -> strongest.
 */
export const RANK_THRESHOLDS: readonly { rank: Rank; min: number; max: number }[] = [
  { rank: 'E', min: 0, max: 19 },
  { rank: 'D', min: 20, max: 34 },
  { rank: 'C', min: 35, max: 49 },
  { rank: 'B', min: 50, max: 64 },
  { rank: 'A', min: 65, max: 79 },
  { rank: 'S', min: 80, max: 100 },
];

/**
 * Accent color per rank (PLAN.txt §7). Rank is NEVER communicated by color
 * alone — always show the letter too (accessibility requirement). These are
 * accents for badges, not full-screen fills.
 */
export const RANK_COLORS: Record<Rank, string> = {
  E: '#8B99AA', // gray
  D: '#4ADE80', // green
  C: '#38E1D6', // cyan
  B: Palette.primary, // blue
  A: Palette.accent, // purple
  S: '#F5C451', // gold
};

/** Clamp a raw number into the valid 0..100 score range. */
export function clampScore(score: number): number {
  if (Number.isNaN(score)) return 0;
  if (score < 0) return 0;
  if (score > 100) return 100;
  return score;
}

/**
 * Map a 0..100 score to its rank. Out-of-range inputs are clamped first, so
 * this always returns a valid Rank (never throws).
 */
export function scoreToRank(score: number): Rank {
  const value = clampScore(score);
  // Thresholds are ordered; return the first band whose max the value is <=.
  for (const band of RANK_THRESHOLDS) {
    if (value <= band.max) return band.rank;
  }
  // clampScore guarantees value <= 100, so the loop always returns; this is a
  // defensive fallback for the strongest rank.
  return 'S';
}

/** The accent color for a given rank. */
export function getRankColor(rank: Rank): string {
  return RANK_COLORS[rank];
}

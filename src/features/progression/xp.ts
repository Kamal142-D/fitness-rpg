/**
 * XP curve and helpers (PLAN.txt §6.10). Pure and deterministic.
 *
 * Level represents activity/account progression (distinct from Hunter rank,
 * which represents demonstrated performance). Economy constants live here, not
 * in the UI, so the curve can be tuned in one place.
 */

/** XP required to advance FROM the given level to the next: round(100 * level^1.5). */
export function getXpRequiredForLevel(level: number): number {
  const l = Math.max(1, Math.floor(level));
  return Math.round(100 * Math.pow(l, 1.5));
}

export interface XpProgress {
  /** XP earned toward the current level (clamped to [0, required]). */
  current: number;
  /** XP needed to reach the next level. */
  required: number;
  /** current / required, in [0, 1]. */
  fraction: number;
}

/** Progress of `currentXp` within `level`. */
export function xpProgress(currentXp: number, level: number): XpProgress {
  const required = getXpRequiredForLevel(level);
  const current = Math.max(0, Math.min(currentXp, required));
  return { current, required, fraction: required > 0 ? current / required : 0 };
}

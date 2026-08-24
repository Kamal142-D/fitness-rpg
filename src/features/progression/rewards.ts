/**
 * XP rewards + level application (PLAN.txt §6.10). Pure. Economy constants live
 * here, outside the UI, so the curve/rewards can be tuned in one place.
 */
import type { Rank } from '@/constants/ranks';
import { getXpRequiredForLevel } from '@/features/progression/xp';

export const XP_REWARDS = {
  completeWorkout: 300,
  perValidWorkingSet: 10,
  perMeaningfulPr: 50,
  /** Gate clear bonus by clear rank (kept within the documented 100–300 band). */
  gateClearBonus: { E: 100, D: 100, C: 150, B: 200, A: 250, S: 300 } as Record<Rank, number>,
} as const;

export interface WorkoutXpInput {
  completed: boolean;
  validWorkingSets: number;
  meaningfulPrCount: number;
  gateClearRank: Rank | null;
}

/** Total XP earned from a finished workout. */
export function xpForWorkout(input: WorkoutXpInput): number {
  let xp = 0;
  if (input.completed) xp += XP_REWARDS.completeWorkout;
  xp += Math.max(0, input.validWorkingSets) * XP_REWARDS.perValidWorkingSet;
  xp += Math.max(0, input.meaningfulPrCount) * XP_REWARDS.perMeaningfulPr;
  if (input.gateClearRank) xp += XP_REWARDS.gateClearBonus[input.gateClearRank];
  return xp;
}

export interface ProgressionSnapshot {
  level: number;
  currentXp: number;
  lifetimeXp: number;
}

export interface XpApplication extends ProgressionSnapshot {
  leveledUp: boolean;
  levelsGained: number;
}

/**
 * Apply earned XP to a progression snapshot, rolling over multiple levels if the
 * gain is large. Deterministic; used both for the Gate Cleared preview and (in
 * Phase 11) for durable persistence.
 */
export function applyXp(current: ProgressionSnapshot, earned: number): XpApplication {
  const gain = Math.max(0, Math.floor(earned));
  let level = Math.max(1, current.level);
  let currentXp = current.currentXp + gain;
  let levelsGained = 0;

  // Guard against pathological input with a hard cap on rollovers.
  while (currentXp >= getXpRequiredForLevel(level) && levelsGained < 1000) {
    currentXp -= getXpRequiredForLevel(level);
    level += 1;
    levelsGained += 1;
  }

  return {
    level,
    currentXp,
    lifetimeXp: current.lifetimeXp + gain,
    leveledUp: levelsGained > 0,
    levelsGained,
  };
}

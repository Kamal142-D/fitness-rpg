/**
 * Streak, adherence, and the Discipline attribute (PLAN.txt §6.9).
 *
 * Rest is respected: a scheduled rest day never breaks a streak, and Discipline
 * does not reward unsafe "never miss a day" behavior (the streak contribution is
 * capped).
 */
import { clampScore } from '@/constants/ranks';

export interface StreakState {
  current: number;
  longest: number;
}

export interface DayOutcome {
  didTrain: boolean;
  /** A planned rest day — must not break the streak. */
  isScheduledRest: boolean;
}

/** Advance a streak by one day's outcome. */
export function updateStreak(prev: StreakState, day: DayOutcome): StreakState {
  if (day.didTrain) {
    const current = prev.current + 1;
    return { current, longest: Math.max(prev.longest, current) };
  }
  if (day.isScheduledRest) {
    return prev; // rest day: streak preserved
  }
  return { current: 0, longest: prev.longest }; // missed a planned training day
}

/** Adherence over a window: completed / planned, clamped to 0..1. */
export function adherenceRatio(completedWorkouts: number, plannedWorkouts: number): number {
  if (plannedWorkouts <= 0) return 0;
  return Math.max(0, Math.min(1, completedWorkouts / plannedWorkouts));
}

export interface DisciplineInput {
  currentStreakDays: number;
  /** 0..1 adherence over a recent window. */
  adherence: number;
}

/**
 * Discipline attribute: mostly adherence, with a capped streak bonus so a long
 * streak can't dominate and unsafe behavior isn't incentivized.
 */
export function disciplineScore(input: DisciplineInput): number {
  const adherencePart = Math.max(0, Math.min(1, input.adherence)) * 75;
  const streakBonus = Math.min(25, Math.max(0, input.currentStreakDays) * 2.5);
  return clampScore(adherencePart + streakBonus);
}

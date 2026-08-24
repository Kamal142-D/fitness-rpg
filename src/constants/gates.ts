import type { Rank } from '@/constants/ranks';

export interface SuggestedGate {
  name: string;
  /** Gate Difficulty (chosen before training) — distinct from Gate Clear Rank. */
  difficulty: Rank;
  muscleGroups: string[];
  durationMinutes: number;
  intensity: string;
}

/**
 * PLACEHOLDER "Today's Gate" suggestion for the System dashboard.
 *
 * Phase 6 replaces this with a real recommendation from the user's
 * workout_templates / gate library. Until then the dashboard shows this single
 * sensible starter so it can answer "what should I do today?" without fabricating
 * user history.
 */
export const STARTER_GATE: SuggestedGate = {
  name: 'Full Body — Initiation',
  difficulty: 'D',
  muscleGroups: ['Chest', 'Back', 'Legs', 'Core'],
  durationMinutes: 45,
  intensity: 'Moderate',
};

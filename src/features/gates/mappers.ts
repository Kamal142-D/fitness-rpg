/**
 * Pure display mappers for Gates. No I/O — easy to unit test.
 */
import type { SuggestedGate } from '@/constants/gates';
import type { Rank } from '@/constants/ranks';
import type { GateTemplate, TemplateExercise } from '@/features/gates/types';

const DEFAULT_DIFFICULTY: Rank = 'D';
const DEFAULT_DURATION = 45;

/** A qualitative intensity label for a Gate Difficulty. */
export function intensityForDifficulty(difficulty: Rank): string {
  switch (difficulty) {
    case 'E':
    case 'D':
      return 'Light';
    case 'C':
      return 'Moderate';
    case 'B':
      return 'Hard';
    case 'A':
    case 'S':
      return 'Brutal';
  }
}

/** Coerce a possibly-null template difficulty string to a valid Rank. */
export function templateDifficulty(t: Pick<GateTemplate, 'difficulty'>): Rank {
  const d = t.difficulty;
  if (d === 'E' || d === 'D' || d === 'C' || d === 'B' || d === 'A' || d === 'S') return d;
  return DEFAULT_DIFFICULTY;
}

/** Split a template description into muscle-group chips. */
export function muscleGroupsFor(t: Pick<GateTemplate, 'description'>): string[] {
  if (!t.description) return [];
  return t.description
    .split(/[,·]/)
    .map((s) => s.trim())
    .filter(Boolean);
}

/** Map a template row to the SuggestedGate shape used by the dashboard GateCard. */
export function templateToSuggestedGate(t: GateTemplate): SuggestedGate {
  const difficulty = templateDifficulty(t);
  return {
    name: t.name,
    difficulty,
    muscleGroups: muscleGroupsFor(t),
    durationMinutes: t.estimated_duration_minutes ?? DEFAULT_DURATION,
    intensity: intensityForDifficulty(difficulty),
  };
}

/** Format a target rep range, e.g. "5-8", "8+", or "—" when unset (time-based). */
export function formatRepRange(min: number | null, max: number | null): string {
  if (min == null && max == null) return '—';
  if (min != null && max != null) return min === max ? `${min}` : `${min}-${max}`;
  if (min != null) return `${min}+`;
  return `up to ${max}`;
}

/** Format a template exercise's targets, e.g. "4 × 5-8". */
export function formatTargets(
  te: Pick<TemplateExercise, 'target_sets' | 'target_reps_min' | 'target_reps_max'>,
): string {
  const sets = te.target_sets ?? 0;
  const reps = formatRepRange(te.target_reps_min, te.target_reps_max);
  if (sets <= 0) return reps === '—' ? '—' : reps;
  return reps === '—' ? `${sets} sets` : `${sets} × ${reps}`;
}

/**
 * Ranking configuration — ALL tunable constants live here, isolated from logic
 * and UI (PLAN.txt §6). Values marked PROVISIONAL are seed estimates, not
 * authoritative population standards; see docs/RANKING_SYSTEM.md for calibration
 * notes. Bump RANKING_VERSION when the math or these constants change.
 */
import type { Anchor } from '@/services/ranking/interp';

export const RANKING_VERSION = 1;

/** Gate Score component weights (PLAN.txt §6.5). Must sum to 1. */
export const GATE_WEIGHTS = {
  performance: 0.5,
  completion: 0.2,
  progress: 0.15,
  pr: 0.1,
  quality: 0.05,
} as const;

/** Hunter Rank attribute weights (PLAN.txt §6.7). Must sum to 1. */
export const HUNTER_WEIGHTS = {
  strength: 0.4,
  physique: 0.3,
  endurance: 0.15,
  discipline: 0.15,
} as const;

/** Neutral score used where a factor has no data yet (don't punish new users). */
export const NEUTRAL_SCORE = 60;

/** Plausibility bounds for anti-inflation validation (PLAN.txt §6.6). */
export const VALIDATION_LIMITS = {
  minWeightKg: 0,
  maxWeightKg: 600,
  minReps: 1,
  maxReps: 100,
  minRpe: 0,
  maxRpe: 10,
  /** A qualifying performance needs at least this many valid working sets. */
  minQualifyingSets: 2,
  /** Reaching S rank must be demonstrated across at least this many sessions. */
  minSessionsForS: 2,
  /** A single rank update may not jump more than this many bands. */
  maxRankJump: 2,
} as const;

/**
 * Score anchors aligned to the rank-band lower bounds (PLAN.txt §6.1) plus the
 * top of the scale. Strength standards give the bodyweight ratio expected at
 * each of these scores.
 */
export const SCORE_ANCHORS = [0, 20, 35, 50, 65, 80, 100] as const;

/**
 * PROVISIONAL strength standards: estimated-1RM as a multiple of bodyweight for
 * a trained MALE lifter, at each SCORE_ANCHOR. Female / neutral sets are derived
 * by scaling (see SEX_SCALE). These are seed values to be calibrated.
 */
const MALE_RATIO_ANCHORS: Record<string, readonly number[]> = {
  bench: [0.2, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0],
  squat: [0.4, 0.75, 1.0, 1.25, 1.5, 1.75, 2.5],
  deadlift: [0.5, 1.0, 1.25, 1.5, 1.75, 2.0, 3.0],
  ohp: [0.15, 0.35, 0.5, 0.6, 0.75, 0.9, 1.3],
  row: [0.3, 0.55, 0.75, 0.95, 1.15, 1.35, 1.8],
};

/** PROVISIONAL sex scaling of the male ratio standards. */
const SEX_SCALE: Record<'male' | 'female' | 'neutral', number> = {
  male: 1.0,
  female: 0.72,
  neutral: 0.86,
};

export type Sex = string | null | undefined;
export type MovementKey = keyof typeof MALE_RATIO_ANCHORS;

/** Map an exercise name to a strength-standard movement, or null if unranked. */
const EXERCISE_MOVEMENT: Record<string, MovementKey> = {
  'Barbell Bench Press': 'bench',
  'Dumbbell Bench Press': 'bench',
  'Incline Dumbbell Press': 'bench',
  'Barbell Back Squat': 'squat',
  'Front Squat': 'squat',
  Deadlift: 'deadlift',
  'Romanian Deadlift': 'deadlift',
  'Overhead Press': 'ohp',
  'Barbell Bent-Over Row': 'row',
};

export function movementForExercise(name: string): MovementKey | null {
  return EXERCISE_MOVEMENT[name] ?? null;
}

function sexKey(sex: Sex): 'male' | 'female' | 'neutral' {
  if (sex === 'male') return 'male';
  if (sex === 'female') return 'female';
  return 'neutral';
}

/** Ratio→score anchors for a movement + sex (scaled male standards). */
export function strengthAnchors(movement: MovementKey, sex: Sex): Anchor[] {
  const scale = SEX_SCALE[sexKey(sex)];
  const ratios = MALE_RATIO_ANCHORS[movement];
  return ratios.map((r, i) => ({ x: r * scale, y: SCORE_ANCHORS[i] }));
}

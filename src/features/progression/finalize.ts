/**
 * Build the durable progression snapshot persisted after a workout (Phase 11).
 * Pure — composes the tested XP curve (applyXp) and the ranking engine (Hunter
 * score/rank). The result is handed to the guarded apply_session_progression RPC.
 *
 * Attributes: a null means "not recomputed this workout" — carry the current
 * value. For the Hunter blend, an attribute of 0 is treated as "no data yet" and
 * renormalized out, so new users get a neutral Hunter score rather than zero.
 */
import type { Rank } from '@/constants/ranks';
import { applyXp, type ProgressionSnapshot } from '@/features/progression/rewards';
import {
  disciplineScore,
  exerciseScore,
  hunterRank,
  hunterScore,
  physiqueScore,
  strengthScore,
} from '@/services/ranking';

export interface AttributeInputs {
  strength: number | null;
  physique: number | null;
  endurance: number | null;
  discipline: number | null;
}

export interface FinishInputs {
  bodyweightKg: number | null;
  sex: string | null;
  exercises: { name: string; best1RMkg: number | null }[];
  assessment: {
    bodyFatPercent: number | null;
    skeletalMuscleMassKg: number | null;
    weightKg: number | null;
    sex: string | null;
  } | null;
}

/**
 * Recompute attributes after a workout from gathered inputs (pure). Strength is
 * the average of ranked exercise scores; physique comes from the latest
 * assessment (null if none); discipline from the streak; endurance is carried.
 */
export function computeAttributes(inputs: FinishInputs, newStreakDays: number): AttributeInputs {
  const scores = inputs.exercises
    .map((e) =>
      exerciseScore({
        exerciseName: e.name,
        bestEstimated1rmKg: e.best1RMkg,
        bodyweightKg: inputs.bodyweightKg,
        sex: inputs.sex,
      }),
    )
    .filter((s): s is number => s != null);

  return {
    strength: strengthScore(scores),
    physique: physiqueScore(inputs.assessment),
    endurance: null,
    discipline: disciplineScore({ currentStreakDays: newStreakDays, adherence: 1 }),
  };
}

export interface ProgressionUpdateInput {
  current: ProgressionSnapshot;
  currentAttributes: { strength: number; physique: number; endurance: number; discipline: number };
  xpEarned: number;
  streak: { current: number; longest: number };
  /** Newly computed attributes; null keeps the current value. */
  attributes: AttributeInputs;
}

export interface ProgressionPersistPayload {
  level: number;
  current_xp: number;
  lifetime_xp: number;
  strength_score: number;
  physique_score: number;
  endurance_score: number;
  discipline_score: number;
  hunter_score: number;
  hunter_rank: Rank;
  current_streak_days: number;
  longest_streak_days: number;
}

export function buildProgressionUpdate(input: ProgressionUpdateInput): ProgressionPersistPayload {
  const xp = applyXp(input.current, input.xpEarned);

  const resolved = {
    strength: input.attributes.strength ?? input.currentAttributes.strength,
    physique: input.attributes.physique ?? input.currentAttributes.physique,
    endurance: input.attributes.endurance ?? input.currentAttributes.endurance,
    discipline: input.attributes.discipline ?? input.currentAttributes.discipline,
  };

  const meaningful = (v: number) => (v > 0 ? v : null);
  const score = hunterScore({
    strength: meaningful(resolved.strength),
    physique: meaningful(resolved.physique),
    endurance: meaningful(resolved.endurance),
    discipline: meaningful(resolved.discipline),
  });

  return {
    level: xp.level,
    current_xp: xp.currentXp,
    lifetime_xp: xp.lifetimeXp,
    strength_score: resolved.strength,
    physique_score: resolved.physique,
    endurance_score: resolved.endurance,
    discipline_score: resolved.discipline,
    hunter_score: score,
    hunter_rank: hunterRank(score),
    current_streak_days: input.streak.current,
    longest_streak_days: input.streak.longest,
  };
}

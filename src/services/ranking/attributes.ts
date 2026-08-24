/**
 * Hunter attributes -> Hunter Rank (PLAN.txt §6.7–6.9). Strength/Physique/
 * Endurance/Discipline each 0..100, combined by HUNTER_WEIGHTS. All models here
 * are PROVISIONAL and use healthy, non-punitive ranges — no medical claims.
 */
import { clampScore, scoreToRank, type Rank } from '@/constants/ranks';
import { HUNTER_WEIGHTS, type Sex } from '@/services/ranking/config';
import { weightedRenormalized } from '@/services/ranking/gateScore';
import { interpolate, type Anchor } from '@/services/ranking/interp';

export interface HunterAttributes {
  strength: number | null;
  physique: number | null;
  endurance: number | null;
  discipline: number | null;
}

/** Strength attribute: average of the user's ranked exercise scores. */
export function strengthScore(exerciseScores: number[]): number | null {
  if (exerciseScores.length === 0) return null;
  const sum = exerciseScores.reduce((a, b) => a + clampScore(b), 0);
  return clampScore(sum / exerciseScores.length);
}

// PROVISIONAL healthy body-fat curves (bodyFat% -> score). Peak in a healthy
// band; not rewarding for ever-lower body fat.
const BODYFAT_MALE: readonly Anchor[] = [
  { x: 4, y: 60 },
  { x: 8, y: 85 },
  { x: 12, y: 100 },
  { x: 18, y: 90 },
  { x: 25, y: 65 },
  { x: 32, y: 40 },
  { x: 40, y: 20 },
];
const BODYFAT_FEMALE: readonly Anchor[] = [
  { x: 12, y: 60 },
  { x: 16, y: 85 },
  { x: 22, y: 100 },
  { x: 28, y: 90 },
  { x: 34, y: 65 },
  { x: 40, y: 40 },
  { x: 48, y: 20 },
];
// PROVISIONAL muscle development: skeletal-muscle mass as % of bodyweight.
const MUSCLE_DEV: readonly Anchor[] = [
  { x: 30, y: 30 },
  { x: 38, y: 60 },
  { x: 44, y: 90 },
  { x: 50, y: 100 },
];

export interface PhysiqueInput {
  bodyFatPercent: number | null;
  skeletalMuscleMassKg: number | null;
  weightKg: number | null;
  sex: Sex;
}

/** Physique attribute from body composition. Null when there is no assessment. */
export function physiqueScore(a: PhysiqueInput | null): number | null {
  if (!a) return null;
  const parts: { value: number | null; weight: number }[] = [];

  if (a.bodyFatPercent != null) {
    const curve = a.sex === 'female' ? BODYFAT_FEMALE : BODYFAT_MALE;
    parts.push({ value: clampScore(interpolate(curve, a.bodyFatPercent)), weight: 0.5 });
  }
  if (a.skeletalMuscleMassKg != null && a.weightKg != null && a.weightKg > 0) {
    const smmPct = (a.skeletalMuscleMassKg / a.weightKg) * 100;
    const scale = a.sex === 'female' ? 1.1 : 1.0; // provisional: women carry less SMM%
    parts.push({ value: clampScore(interpolate(MUSCLE_DEV, smmPct * scale)), weight: 0.5 });
  }
  if (parts.length === 0) return null;
  return weightedRenormalized(parts);
}

// PROVISIONAL endurance from weekly training minutes (guideline ~150 min).
const ENDURANCE_MINUTES: readonly Anchor[] = [
  { x: 0, y: 0 },
  { x: 75, y: 45 },
  { x: 150, y: 70 },
  { x: 300, y: 100 },
];

/** Endurance attribute from recent training volume. Null when no data. */
export function enduranceScore(weeklyTrainingMinutes: number | null): number | null {
  if (weeklyTrainingMinutes == null) return null;
  return clampScore(interpolate(ENDURANCE_MINUTES, weeklyTrainingMinutes));
}

/** Combined Hunter score, renormalized over available attributes. */
export function hunterScore(attrs: HunterAttributes): number {
  return weightedRenormalized([
    { value: attrs.strength, weight: HUNTER_WEIGHTS.strength },
    { value: attrs.physique, weight: HUNTER_WEIGHTS.physique },
    { value: attrs.endurance, weight: HUNTER_WEIGHTS.endurance },
    { value: attrs.discipline, weight: HUNTER_WEIGHTS.discipline },
  ]);
}

export function hunterRank(score: number): Rank {
  return scoreToRank(score);
}

export type AttributeName = keyof HunterAttributes;

/** The lowest available attribute — the one most limiting the next rank. */
export function limitingAttribute(attrs: HunterAttributes): AttributeName | null {
  let name: AttributeName | null = null;
  let lowest = Infinity;
  (Object.keys(attrs) as AttributeName[]).forEach((k) => {
    const v = attrs[k];
    if (v != null && v < lowest) {
      lowest = v;
      name = k;
    }
  });
  return name;
}

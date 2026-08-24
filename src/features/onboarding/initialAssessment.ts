/**
 * PROVISIONAL initial assessment (Phase 4).
 *
 * Produces a first-pass set of attribute scores + Hunter rank from onboarding
 * answers, so the Awakening can end on a meaningful rank reveal. This is NOT the
 * authoritative ranking engine (that is Phase 9) and does NOT use validated
 * population standards — the constants below are provisional, seeded estimates,
 * isolated here so they can be recalibrated without touching UI. Every output is
 * clamped to 0..100 and the Hunter weighting follows PLAN.txt §6.7.
 */
import { clampScore, scoreToRank, type Rank } from '@/constants/ranks';
import type { ExperienceLevel, OnboardingDraft, Sex } from '@/features/onboarding/types';

export const INITIAL_ASSESSMENT_VERSION = 'provisional-1';

const CONFIG = {
  /** Baseline attribute scores by training experience (provisional). */
  experience: {
    beginner: { strength: 22, physique: 32, endurance: 26 },
    intermediate: { strength: 42, physique: 46, endurance: 42 },
    advanced: { strength: 60, physique: 58, endurance: 56 },
  },
  /**
   * Relative-strength reference: (sum of big-3 1RM) / bodyweight -> score.
   * Piecewise-linear, provisional. Only used when all three lifts are provided.
   */
  relStrength: [
    { ratio: 2, score: 20 },
    { ratio: 4, score: 45 },
    { ratio: 6, score: 65 },
    { ratio: 8, score: 82 },
    { ratio: 10, score: 94 },
  ],
  /** Healthy, non-punitive body-fat bands (percent). Score peaks inside the band. */
  bodyFat: {
    male: { low: 10, high: 18 },
    female: { low: 18, high: 26 },
    default: { low: 13, high: 23 },
  },
  /**
   * Discipline by planned training days/week. 3-5 is the sweet spot; 6-7 is NOT
   * extra-rewarded, to avoid encouraging unsafe "never rest" behavior (§6.9).
   */
  disciplineByDays: [15, 30, 42, 55, 62, 68, 66, 62] as const,
  /** Hunter score weighting (PLAN.txt §6.7). */
  hunterWeights: { strength: 0.4, physique: 0.3, endurance: 0.15, discipline: 0.15 },
  /** Non-punitive floor for a very-out-of-range body-fat value. */
  bodyFatFloor: 42,
} as const;

export interface InitialAssessment {
  strength: number;
  physique: number;
  endurance: number;
  discipline: number;
  hunterScore: number;
  hunterRank: Rank;
  version: string;
}

function experienceBase(level: ExperienceLevel | null) {
  return CONFIG.experience[level ?? 'beginner'];
}

/** Interpolate a relative-strength ratio to a 0..100 score. */
function relStrengthScore(ratio: number): number {
  const pts = CONFIG.relStrength;
  if (ratio <= pts[0].ratio) return pts[0].score;
  if (ratio >= pts[pts.length - 1].ratio) return pts[pts.length - 1].score;
  for (let i = 0; i < pts.length - 1; i++) {
    const a = pts[i];
    const b = pts[i + 1];
    if (ratio >= a.ratio && ratio <= b.ratio) {
      const t = (ratio - a.ratio) / (b.ratio - a.ratio);
      return a.score + t * (b.score - a.score);
    }
  }
  return pts[pts.length - 1].score;
}

function bodyFatBand(sex: Sex | null) {
  if (sex === 'male') return CONFIG.bodyFat.male;
  if (sex === 'female') return CONFIG.bodyFat.female;
  return CONFIG.bodyFat.default;
}

/** Score a body-fat percentage against a healthy band (100 inside, tapering out). */
function bodyFatScore(bf: number, sex: Sex | null): number {
  const band = bodyFatBand(sex);
  if (bf >= band.low && bf <= band.high) return 100;
  const distance = bf < band.low ? band.low - bf : bf - band.high;
  return Math.max(CONFIG.bodyFatFloor, 100 - distance * 3);
}

/**
 * Compute the provisional initial assessment from an onboarding draft.
 * Missing optional inputs fall back to experience-based baselines.
 */
export function computeInitialAssessment(draft: OnboardingDraft): InitialAssessment {
  const base = experienceBase(draft.experience_level);

  // Strength: use relative strength only when all three lifts + bodyweight exist.
  let strength: number = base.strength;
  const { baseline_bench_kg, baseline_squat_kg, baseline_deadlift_kg, current_weight_kg } = draft;
  if (
    baseline_bench_kg &&
    baseline_squat_kg &&
    baseline_deadlift_kg &&
    current_weight_kg &&
    current_weight_kg > 0
  ) {
    const ratio =
      (baseline_bench_kg + baseline_squat_kg + baseline_deadlift_kg) / current_weight_kg;
    strength = relStrengthScore(ratio);
  }

  // Endurance: experience base + a small bump for more planned days.
  const days = draft.training_days_per_week ?? 0;
  const endurance = clampScore(base.endurance + Math.min(days, 6));

  // Discipline: from planned days (sweet-spot curve).
  const discipline = clampScore(CONFIG.disciplineByDays[Math.max(0, Math.min(7, days))]);

  // Physique: experience base, refined by body-fat if provided (non-punitive),
  // plus a small boost for a good muscle-to-weight ratio.
  let physique: number = base.physique;
  if (draft.body_fat_percent != null) {
    const s = bodyFatScore(draft.body_fat_percent, draft.sex);
    physique = 0.55 * base.physique + 0.45 * s;
  }
  if (draft.skeletal_muscle_mass_kg != null && current_weight_kg && current_weight_kg > 0) {
    const ratio = draft.skeletal_muscle_mass_kg / current_weight_kg;
    physique += Math.max(0, Math.min(12, (ratio - 0.35) * 100));
  }
  physique = clampScore(physique);

  const w = CONFIG.hunterWeights;
  const hunterScore = clampScore(
    strength * w.strength +
      physique * w.physique +
      endurance * w.endurance +
      discipline * w.discipline,
  );

  return {
    strength: clampScore(strength),
    physique,
    endurance,
    discipline,
    hunterScore,
    hunterRank: scoreToRank(hunterScore),
    version: INITIAL_ASSESSMENT_VERSION,
  };
}

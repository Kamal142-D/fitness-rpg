/**
 * Personal-record detection (Phase 8). Pure and deterministic.
 *
 * Rules:
 * - Only completed WORKING sets count (warm-ups never set records).
 * - Four record types: weight, reps, estimated 1RM, volume (per-exercise session
 *   total). At most ONE PR per type per exercise (dedup) — the best set.
 * - First-ever attempt establishes a BASELINE, not a PR (anti-spam): a record is
 *   only a PR when it strictly beats a prior best.
 * - `stats` always reports the new all-time bests (max of prior and this
 *   session), even when nothing was a PR, so exercise_user_stats stays current.
 */
import type {
  DetectExercise,
  DetectResult,
  DetectedPR,
  NewStat,
  PriorStat,
  RecordType,
} from '@/features/pr/types';

const EPS = 0.01;

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

function maxN(a: number | null | undefined, b: number | null | undefined): number | null {
  const av = a ?? null;
  const bv = b ?? null;
  if (av == null) return bv;
  if (bv == null) return av;
  return Math.max(av, bv);
}

/** A value is a PR only if there was a prior best AND it strictly beats it. */
function isPR(newValue: number, prior: number | null | undefined): boolean {
  if (prior == null) return false; // first time: baseline, not a PR
  return newValue > prior + EPS;
}

export function detectPRs(
  exercises: DetectExercise[],
  priorStats: Record<string, PriorStat | undefined>,
): DetectResult {
  const prs: DetectedPR[] = [];
  const stats: NewStat[] = [];

  for (const ex of exercises) {
    const working = ex.sets.filter((s) => !s.isWarmup);
    if (working.length === 0) continue;

    let bestWeightSet: (typeof working)[number] | null = null;
    let bestRepsSet: (typeof working)[number] | null = null;
    let best1rmSet: (typeof working)[number] | null = null;
    let volume = 0;

    for (const s of working) {
      if (s.weightKg != null && s.weightKg > 0) {
        if (!bestWeightSet || s.weightKg > (bestWeightSet.weightKg ?? 0)) bestWeightSet = s;
      }
      if (s.reps != null && s.reps > 0) {
        if (!bestRepsSet || s.reps > (bestRepsSet.reps ?? 0)) bestRepsSet = s;
      }
      if (s.est1RM != null) {
        if (!best1rmSet || s.est1RM > (best1rmSet.est1RM ?? 0)) best1rmSet = s;
      }
      volume += (s.weightKg ?? 0) * (s.reps ?? 0);
    }

    const prior = priorStats[ex.exerciseId];
    const sessWeight = bestWeightSet?.weightKg ?? null;
    const sessReps = bestRepsSet?.reps ?? null;
    const sess1rm = best1rmSet != null ? round2(best1rmSet.est1RM as number) : null;
    const sessVolume = volume > 0 ? round2(volume) : null;

    const push = (
      recordType: RecordType,
      setNumber: number,
      previousValue: number | null,
      newValue: number,
    ) =>
      prs.push({
        exerciseId: ex.exerciseId,
        orderIndex: ex.orderIndex,
        setNumber,
        recordType,
        previousValue,
        newValue,
      });

    if (sessWeight != null && isPR(sessWeight, prior?.bestWeightKg)) {
      push('weight', bestWeightSet!.setNumber, prior?.bestWeightKg ?? null, sessWeight);
    }
    if (sessReps != null && isPR(sessReps, prior?.bestReps)) {
      push('reps', bestRepsSet!.setNumber, prior?.bestReps ?? null, sessReps);
    }
    if (sess1rm != null && isPR(sess1rm, prior?.bestEstimated1rmKg)) {
      push('estimated_1rm', best1rmSet!.setNumber, prior?.bestEstimated1rmKg ?? null, sess1rm);
    }
    if (sessVolume != null && isPR(sessVolume, prior?.bestVolumeKg)) {
      push(
        'volume',
        working[working.length - 1].setNumber,
        prior?.bestVolumeKg ?? null,
        sessVolume,
      );
    }

    stats.push({
      exerciseId: ex.exerciseId,
      bestWeightKg: maxN(prior?.bestWeightKg, sessWeight),
      bestReps: maxN(prior?.bestReps, sessReps),
      bestEstimated1rmKg: maxN(prior?.bestEstimated1rmKg, sess1rm),
      bestVolumeKg: maxN(prior?.bestVolumeKg, sessVolume),
    });
  }

  return { prs, stats };
}

const PRIORITY: Record<RecordType, number> = {
  estimated_1rm: 0, // most meaningful strength progress first
  weight: 1,
  reps: 2,
  volume: 3,
};

/**
 * Order PRs by importance (estimated-1RM first) and, within a type, by the size
 * of the improvement. Optionally cap the count to keep celebrations from
 * spamming. Does not drop data — persistence keeps all PRs; this is for display.
 */
export function prioritizePRs(prs: DetectedPR[], limit?: number): DetectedPR[] {
  const improvement = (p: DetectedPR) =>
    p.previousValue == null ? p.newValue : p.newValue - p.previousValue;
  const sorted = [...prs].sort((a, b) => {
    if (PRIORITY[a.recordType] !== PRIORITY[b.recordType]) {
      return PRIORITY[a.recordType] - PRIORITY[b.recordType];
    }
    return improvement(b) - improvement(a);
  });
  return limit == null ? sorted : sorted.slice(0, limit);
}

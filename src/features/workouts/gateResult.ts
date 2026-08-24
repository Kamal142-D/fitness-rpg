/**
 * Compute the post-workout Gate result (Phase 10): Gate Score -> Gate Clear Rank,
 * per-exercise performance grades, and XP. Pure — composes the ranking engine
 * (services/ranking) and the XP economy (features/progression/rewards).
 */
import type { Rank } from '@/constants/ranks';
import type { PriorStat } from '@/features/pr/types';
import { xpForWorkout } from '@/features/progression/rewards';
import type { CompletionAggregates, CompletionPayload } from '@/features/workouts/payload';
import {
  completionScore,
  computeGateScore,
  gateClearRank,
  performanceGrade,
  performanceScore,
  prComponentScore,
  qualityScore,
} from '@/services/ranking';

export interface PerExerciseResult {
  exerciseId: string;
  performanceScore: number;
  performanceGrade: Rank;
}

export interface GateResult {
  gateScore: number;
  gateClearRank: Rank;
  completionScore: number;
  performanceScore: number;
  qualityScore: number;
  progressScore: number | null;
  xpEarned: number;
  perExercise: PerExerciseResult[];
}

export interface DetectedPrLike {
  recordType: 'weight' | 'reps' | 'estimated_1rm' | 'volume';
}

export function computeGateResult(
  payload: CompletionPayload,
  priorStats: Record<string, PriorStat | undefined>,
  aggregates: CompletionAggregates,
  prs: DetectedPrLike[],
): GateResult {
  const perExercise: PerExerciseResult[] = [];
  const rpes: (number | null)[] = [];

  for (const ex of payload.exercises) {
    const working = ex.sets.filter((s) => !s.is_warmup);
    for (const s of working) rpes.push(s.rpe);

    const todayBest = working.reduce<number | null>((best, s) => {
      if (s.estimated_1rm_kg == null) return best;
      return best == null ? s.estimated_1rm_kg : Math.max(best, s.estimated_1rm_kg);
    }, null);

    const baseline = priorStats[ex.exercise_id]?.bestEstimated1rmKg ?? null;
    const pScore = performanceScore(todayBest, baseline);
    perExercise.push({
      exerciseId: ex.exercise_id,
      performanceScore: pScore,
      performanceGrade: performanceGrade(pScore),
    });
  }

  const performanceAvg =
    perExercise.length > 0
      ? perExercise.reduce((a, p) => a + p.performanceScore, 0) / perExercise.length
      : null;

  const completion = completionScore(aggregates.completedSets, aggregates.plannedWorkingSets);
  const quality = qualityScore(rpes);
  // Progress vs recent sessions needs session history; deferred (renormalized out).
  const progress = null;
  const pr = prComponentScore(prs.length);

  const gateScore = computeGateScore({
    performance: performanceAvg,
    completion,
    progress,
    pr,
    quality,
  });
  const rank = gateClearRank(gateScore);

  const meaningfulPrCount = prs.filter(
    (p) => p.recordType === 'estimated_1rm' || p.recordType === 'weight',
  ).length;
  const xpEarned = xpForWorkout({
    completed: true,
    validWorkingSets: aggregates.completedSets,
    meaningfulPrCount,
    gateClearRank: rank,
  });

  return {
    gateScore,
    gateClearRank: rank,
    completionScore: completion,
    performanceScore: performanceAvg ?? 60,
    qualityScore: quality,
    progressScore: progress,
    xpEarned,
    perExercise,
  };
}

import type { DetectedPR, NewStat, PriorStat } from '@/features/pr/types';
import { supabase } from '@/services/supabase';
import type { Json } from '@/types/database';

/** Fetch the user's prior bests for the given exercises (RLS-scoped to them). */
export async function getExerciseStats(exerciseIds: string[]): Promise<Record<string, PriorStat>> {
  if (exerciseIds.length === 0) return {};
  const { data, error } = await supabase
    .from('exercise_user_stats')
    .select('exercise_id, best_weight_kg, best_reps, best_estimated_1rm_kg, best_volume_kg')
    .in('exercise_id', exerciseIds);
  if (error) throw error;

  const map: Record<string, PriorStat> = {};
  for (const r of data ?? []) {
    map[r.exercise_id] = {
      bestWeightKg: r.best_weight_kg,
      bestReps: r.best_reps,
      bestEstimated1rmKg: r.best_estimated_1rm_kg,
      bestVolumeKg: r.best_volume_kg,
    };
  }
  return map;
}

function prToJson(pr: DetectedPR) {
  return {
    exercise_id: pr.exerciseId,
    order_index: pr.orderIndex,
    set_number: pr.setNumber,
    record_type: pr.recordType,
    previous_value: pr.previousValue,
    new_value: pr.newValue,
  };
}

function statToJson(s: NewStat) {
  return {
    exercise_id: s.exerciseId,
    best_weight_kg: s.bestWeightKg,
    best_reps: s.bestReps,
    best_estimated_1rm_kg: s.bestEstimated1rmKg,
    best_volume_kg: s.bestVolumeKg,
  };
}

/** Persist detected PRs + updated stats atomically/idempotently via the RPC. */
export async function applyWorkoutResults(
  sessionId: string,
  prs: DetectedPR[],
  stats: NewStat[],
): Promise<void> {
  const { error } = await supabase.rpc('apply_workout_results', {
    p_session_id: sessionId,
    p_prs: prs.map(prToJson) as unknown as Json,
    p_stats: stats.map(statToJson) as unknown as Json,
  });
  if (error) throw error;
}

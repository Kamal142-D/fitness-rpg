import type {
  ExerciseStatInput,
  PrHistoryItem,
  SessionSummary,
  WeightPoint,
} from '@/features/analytics/types';
import { supabase } from '@/services/supabase';

export interface PlayerData {
  sessions: SessionSummary[];
  stats: ExerciseStatInput[];
  bodyweightKg: number | null;
  sex: string | null;
  prs: PrHistoryItem[];
  weights: WeightPoint[];
}

/** One parallel fetch of everything the Player screen needs (all RLS-scoped). */
export async function getPlayerData(userId: string): Promise<PlayerData> {
  const [sessionsRes, statsRes, exercisesRes, profileRes, prsRes, weightsRes] = await Promise.all([
    supabase
      .from('workout_sessions')
      .select('id, name, completed_at, gate_clear_rank, total_volume_kg, duration_seconds')
      .eq('status', 'completed')
      .order('completed_at', { ascending: false })
      .limit(60),
    supabase.from('exercise_user_stats').select('exercise_id, best_estimated_1rm_kg'),
    supabase.from('exercises').select('id, name'),
    supabase.from('profiles').select('sex, current_weight_kg').eq('id', userId).maybeSingle(),
    supabase
      .from('personal_records')
      .select('id, exercise_id, record_type, new_value, achieved_at')
      .order('achieved_at', { ascending: false })
      .limit(20),
    supabase
      .from('body_assessments')
      .select('weight_kg, assessment_date')
      .order('assessment_date', { ascending: true }),
  ]);

  const nameById = new Map((exercisesRes.data ?? []).map((e) => [e.id, e.name]));

  const sessions: SessionSummary[] = (sessionsRes.data ?? []).map((s) => ({
    id: s.id,
    name: s.name,
    completedAt: s.completed_at,
    gateClearRank: s.gate_clear_rank,
    totalVolumeKg: s.total_volume_kg,
    durationSeconds: s.duration_seconds,
  }));

  const stats: ExerciseStatInput[] = (statsRes.data ?? []).map((s) => ({
    exerciseId: s.exercise_id,
    name: nameById.get(s.exercise_id) ?? '',
    best1RMkg: s.best_estimated_1rm_kg,
  }));

  const prs: PrHistoryItem[] = (prsRes.data ?? []).map((p) => ({
    id: p.id,
    exerciseName: nameById.get(p.exercise_id) ?? 'Exercise',
    recordType: p.record_type,
    newValue: Number(p.new_value),
    achievedAt: p.achieved_at,
  }));

  const weights: WeightPoint[] = (weightsRes.data ?? [])
    .filter((w) => w.weight_kg != null)
    .map((w) => ({ date: w.assessment_date, weightKg: Number(w.weight_kg) }));

  return {
    sessions,
    stats,
    bodyweightKg: profileRes.data?.current_weight_kg ?? null,
    sex: profileRes.data?.sex ?? null,
    prs,
    weights,
  };
}

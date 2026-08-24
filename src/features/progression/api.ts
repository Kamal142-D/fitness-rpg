import type { FinishInputs, ProgressionPersistPayload } from '@/features/progression/finalize';
import { supabase } from '@/services/supabase';
import type { Json, Tables } from '@/types/database';

export type Progression = Tables<'player_progression'>;

/** Fetch the current user's progression row (or null if not provisioned yet). */
export async function getProgression(userId: string): Promise<Progression | null> {
  const { data, error } = await supabase
    .from('player_progression')
    .select('*')
    .eq('user_id', userId)
    .maybeSingle();
  if (error) throw error;
  return data;
}

/** Persist a computed progression snapshot for a session (guarded, idempotent). */
export async function applySessionProgression(
  sessionId: string,
  payload: ProgressionPersistPayload,
): Promise<void> {
  const { error } = await supabase.rpc('apply_session_progression', {
    p_session_id: sessionId,
    p: payload as unknown as Json,
  });
  if (error) throw error;
}

/**
 * Gather the inputs needed to recompute attributes after a workout: profile
 * bodyweight/sex, per-exercise bests, and the latest body assessment.
 */
export async function getFinishInputs(userId: string): Promise<FinishInputs> {
  const [profileRes, statsRes, exercisesRes, assessmentRes] = await Promise.all([
    supabase.from('profiles').select('sex, current_weight_kg').eq('id', userId).maybeSingle(),
    supabase.from('exercise_user_stats').select('exercise_id, best_estimated_1rm_kg'),
    supabase.from('exercises').select('id, name'),
    supabase
      .from('body_assessments')
      .select('body_fat_percent, skeletal_muscle_mass_kg, weight_kg')
      .eq('user_id', userId)
      .order('assessment_date', { ascending: false })
      .limit(1)
      .maybeSingle(),
  ]);

  const sex = profileRes.data?.sex ?? null;
  const bodyweightKg = profileRes.data?.current_weight_kg ?? null;

  const nameById = new Map((exercisesRes.data ?? []).map((e) => [e.id, e.name]));
  const exercises = (statsRes.data ?? []).map((s) => ({
    name: nameById.get(s.exercise_id) ?? '',
    best1RMkg: s.best_estimated_1rm_kg,
  }));

  const a = assessmentRes.data;
  const assessment = a
    ? {
        bodyFatPercent: a.body_fat_percent,
        skeletalMuscleMassKg: a.skeletal_muscle_mass_kg,
        weightKg: a.weight_kg,
        sex,
      }
    : null;

  return { bodyweightKg, sex, exercises, assessment };
}

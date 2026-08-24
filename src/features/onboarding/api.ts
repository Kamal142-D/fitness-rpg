import { friendlyAuthError } from '@/features/auth/errors';
import type { InitialAssessment } from '@/features/onboarding/initialAssessment';
import type { OnboardingDraft } from '@/features/onboarding/types';
import { isSupabaseConfigured, supabase } from '@/services/supabase';
import type { Tables, TablesInsert, TablesUpdate } from '@/types/database';

export type Profile = Tables<'profiles'>;

export type PersistResult = { ok: true } | { ok: false; message: string };

const NOT_CONFIGURED =
  'The app is not connected to a server yet. Add your Supabase keys to .env, then restart.';

/** Fetch the current user's profile row (or null if it does not exist yet). */
export async function getProfile(userId: string): Promise<Profile | null> {
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .maybeSingle();
  if (error) throw error;
  return data;
}

/**
 * Persist a completed Awakening: profile fields + onboarding flag, an optional
 * body-composition assessment, and the initial progression attributes.
 *
 * Runs as sequential writes (the mobile client has no transaction); each is
 * checked. The profile and progression rows already exist (created by the
 * handle_new_user trigger), so these are updates.
 */
export async function completeOnboarding(
  userId: string,
  draft: OnboardingDraft,
  assessment: InitialAssessment,
): Promise<PersistResult> {
  if (!isSupabaseConfigured) return { ok: false, message: NOT_CONFIGURED };

  const profileUpdate: TablesUpdate<'profiles'> = {
    display_name: draft.display_name.trim(),
    date_of_birth: draft.date_of_birth,
    sex: draft.sex,
    height_cm: draft.height_cm,
    current_weight_kg: draft.current_weight_kg,
    experience_level: draft.experience_level,
    fitness_goal: draft.fitness_goal,
    training_days_per_week: draft.training_days_per_week,
    training_location: draft.training_location,
    preferred_workout_minutes: draft.preferred_workout_minutes,
    onboarding_completed: true,
  };

  const { error: profileError } = await supabase
    .from('profiles')
    .update(profileUpdate)
    .eq('id', userId);
  if (profileError) return { ok: false, message: friendlyAuthError(profileError.message) };

  if (draft.body_fat_percent != null || draft.skeletal_muscle_mass_kg != null) {
    const assessmentRow: TablesInsert<'body_assessments'> = {
      user_id: userId,
      weight_kg: draft.current_weight_kg,
      body_fat_percent: draft.body_fat_percent,
      skeletal_muscle_mass_kg: draft.skeletal_muscle_mass_kg,
      source: 'manual',
    };
    const { error: assessmentError } = await supabase
      .from('body_assessments')
      .insert(assessmentRow);
    if (assessmentError) return { ok: false, message: friendlyAuthError(assessmentError.message) };
  }

  const progressionUpdate: TablesUpdate<'player_progression'> = {
    strength_score: assessment.strength,
    physique_score: assessment.physique,
    endurance_score: assessment.endurance,
    discipline_score: assessment.discipline,
    hunter_score: assessment.hunterScore,
    hunter_rank: assessment.hunterRank,
  };
  const { error: progressionError } = await supabase
    .from('player_progression')
    .update(progressionUpdate)
    .eq('user_id', userId);
  if (progressionError) return { ok: false, message: friendlyAuthError(progressionError.message) };

  return { ok: true };
}

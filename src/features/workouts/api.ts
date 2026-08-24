import type { CompletionPayload } from '@/features/workouts/payload';
import { supabase } from '@/services/supabase';
import type { Json } from '@/types/database';

/**
 * Persist a finished workout atomically via the complete_workout RPC. Returns
 * the session id. Idempotent: retrying with the same payload (same session id)
 * returns the id without creating duplicates.
 */
export async function completeWorkout(payload: CompletionPayload): Promise<string> {
  const { data, error } = await supabase.rpc('complete_workout', {
    payload: payload as unknown as Json,
  });
  if (error) throw error;
  return data;
}

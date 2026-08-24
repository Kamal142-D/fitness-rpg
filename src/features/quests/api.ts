import type { UserQuestView } from '@/features/quests/types';
import { supabase } from '@/services/supabase';

/** Assign the current period's daily + weekly quests (idempotent). */
export async function ensureActiveQuests(): Promise<void> {
  const { error } = await supabase.rpc('ensure_active_quests', {});
  if (error) throw error;
}

/** Advance quest progress from a completed workout (idempotent per session). */
export async function recordWorkoutForQuests(sessionId: string): Promise<void> {
  const { error } = await supabase.rpc('record_workout_for_quests', { p_session_id: sessionId });
  if (error) throw error;
}

/** Claim a completed quest's reward (idempotent). */
export async function claimQuest(userQuestId: string): Promise<void> {
  const { error } = await supabase.rpc('claim_quest', { p_user_quest_id: userQuestId });
  if (error) throw error;
}

/**
 * Ensure the current quests exist, then list them joined with their definitions.
 * (Join is done in JS since the generated types carry no relationship metadata.)
 */
export async function listUserQuests(): Promise<UserQuestView[]> {
  await ensureActiveQuests();

  const nowIso = new Date().toISOString();
  const { data: userQuests, error: uqErr } = await supabase
    .from('user_quests')
    .select('id, quest_id, progress, completed, claimed, expires_at')
    .gt('expires_at', nowIso)
    .order('assigned_at', { ascending: false });
  if (uqErr) throw uqErr;
  if (!userQuests || userQuests.length === 0) return [];

  const questIds = [...new Set(userQuests.map((q) => q.quest_id))];
  const { data: quests, error: qErr } = await supabase
    .from('quests')
    .select('id, name, description, type, requirement_value, xp_reward')
    .in('id', questIds);
  if (qErr) throw qErr;
  const byId = new Map((quests ?? []).map((q) => [q.id, q]));

  return userQuests
    .filter((uq) => byId.has(uq.quest_id))
    .map((uq) => {
      const def = byId.get(uq.quest_id)!;
      return {
        id: uq.id,
        questId: uq.quest_id,
        name: def.name,
        description: def.description,
        type: def.type,
        requirementValue: Number(def.requirement_value),
        progress: Number(uq.progress),
        completed: uq.completed,
        claimed: uq.claimed,
        xpReward: def.xp_reward,
        expiresAt: uq.expires_at,
      };
    });
}

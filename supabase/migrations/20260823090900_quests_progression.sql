-- ============================================================================
-- Phase 11: Quests + durable, idempotent progression.
--
-- Idempotency strategy:
--   * workout_sessions.progression_applied / quests_applied flags make the
--     per-session XP/streak/quest-progress updates safe to retry (apply-once).
--   * user_quests.claimed makes quest reward claiming idempotent.
-- The XP level curve lives here (xp_required_for_level) so quest claims can
-- recompute level server-side; it mirrors features/progression/xp.ts.
-- ============================================================================

alter table public.workout_sessions
  add column if not exists progression_applied boolean not null default false,
  add column if not exists quests_applied boolean not null default false;

-- Seed quest definitions (daily + weekly). Idempotent by unique name.
alter table public.quests add constraint quests_name_key unique (name);

insert into public.quests (name, description, type, requirement_type, requirement_value, xp_reward)
values
  ('Enter a Gate', 'Complete 1 workout today', 'daily', 'complete_workouts', 1, 100),
  ('Set Machine', 'Complete 15 working sets today', 'daily', 'complete_sets', 15, 100),
  ('Daily Tonnage', 'Lift 5,000 kg of volume today', 'daily', 'total_volume_kg', 5000, 100),
  ('Weekly Warrior', 'Complete 4 workouts this week', 'weekly', 'complete_workouts', 4, 500),
  ('Record Breaker', 'Earn 3 personal records this week', 'weekly', 'earn_prs', 3, 500),
  ('Iron Week', 'Lift 30,000 kg of volume this week', 'weekly', 'total_volume_kg', 30000, 500)
on conflict (name) do nothing;

-- ----------------------------------------------------------------------------
-- Level curve (mirror of getXpRequiredForLevel): round(100 * level^1.5).
-- ----------------------------------------------------------------------------
create or replace function public.xp_required_for_level(p_level int)
returns int
language sql
immutable
as $$
  select round(100 * power(greatest(p_level, 1), 1.5))::int;
$$;

-- Award XP to a user and roll their level up as far as it goes.
create or replace function public.award_xp(p_user uuid, p_amount int)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_level int;
  v_xp    int;
begin
  if p_amount <= 0 then return; end if;
  update public.player_progression
    set current_xp = current_xp + p_amount,
        lifetime_xp = lifetime_xp + p_amount,
        updated_at = now()
    where user_id = p_user;

  loop
    select level, current_xp into v_level, v_xp
      from public.player_progression where user_id = p_user;
    exit when v_xp < public.xp_required_for_level(v_level);
    update public.player_progression
      set level = v_level + 1, current_xp = v_xp - public.xp_required_for_level(v_level)
      where user_id = p_user;
  end loop;
end;
$$;

-- ----------------------------------------------------------------------------
-- Assign the current period's daily + weekly quests (idempotent).
-- ----------------------------------------------------------------------------
create or replace function public.ensure_active_quests()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  q      record;
  v_exp  timestamptz;
begin
  if v_user is null then raise exception 'not authenticated'; end if;

  for q in select * from public.quests where active and type in ('daily', 'weekly')
  loop
    v_exp := case q.type
      when 'daily' then date_trunc('day', now()) + interval '1 day'
      else date_trunc('week', now()) + interval '1 week'
    end;

    if not exists (
      select 1 from public.user_quests
      where user_id = v_user and quest_id = q.id and expires_at > now()
    ) then
      insert into public.user_quests (user_id, quest_id, progress, expires_at)
      values (v_user, q.id, 0, v_exp);
    end if;
  end loop;
end;
$$;

-- ----------------------------------------------------------------------------
-- Advance quest progress from a completed workout (guarded, apply-once).
-- ----------------------------------------------------------------------------
create or replace function public.record_workout_for_quests(p_session_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user    uuid := auth.uid();
  v_owner   uuid;
  v_applied boolean;
  v_sets    int;
  v_volume  numeric;
  v_prs     int;
  r         record;
  v_inc     numeric;
begin
  if v_user is null then raise exception 'not authenticated'; end if;

  select user_id, quests_applied, coalesce(total_volume_kg, 0)
    into v_owner, v_applied, v_volume
    from public.workout_sessions where id = p_session_id for update;
  if v_owner is null then raise exception 'session not found'; end if;
  if v_owner <> v_user then raise exception 'not your session'; end if;
  if v_applied then return; end if;

  select count(*) into v_sets
    from public.workout_exercises we
    join public.workout_sets ws on ws.workout_exercise_id = we.id
    where we.session_id = p_session_id and ws.is_completed and not ws.is_warmup;

  select count(*) into v_prs
    from public.personal_records pr
    join public.workout_sets ws on ws.id = pr.workout_set_id
    join public.workout_exercises we on we.id = ws.workout_exercise_id
    where we.session_id = p_session_id;

  for r in
    select uq.id, q.requirement_type, q.requirement_value, uq.progress
    from public.user_quests uq
    join public.quests q on q.id = uq.quest_id
    where uq.user_id = v_user and not uq.completed and uq.expires_at > now()
  loop
    v_inc := case r.requirement_type
      when 'complete_workouts' then 1
      when 'complete_sets' then v_sets
      when 'total_volume_kg' then v_volume
      when 'earn_prs' then v_prs
      else 0
    end;

    update public.user_quests
      set progress = r.progress + v_inc,
          completed = (r.progress + v_inc) >= r.requirement_value,
          completed_at = case when (r.progress + v_inc) >= r.requirement_value then now() else completed_at end
      where id = r.id;
  end loop;

  update public.workout_sessions set quests_applied = true where id = p_session_id;
end;
$$;

-- ----------------------------------------------------------------------------
-- Persist a client-computed progression snapshot for a session (guarded).
-- The snapshot's level/XP come from the tested TS engine (applyXp).
-- ----------------------------------------------------------------------------
create or replace function public.apply_session_progression(p_session_id uuid, p jsonb)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user    uuid := auth.uid();
  v_owner   uuid;
  v_applied boolean;
begin
  if v_user is null then raise exception 'not authenticated'; end if;

  select user_id, progression_applied into v_owner, v_applied
    from public.workout_sessions where id = p_session_id for update;
  if v_owner is null then raise exception 'session not found'; end if;
  if v_owner <> v_user then raise exception 'not your session'; end if;
  if v_applied then return; end if;

  update public.player_progression set
    level = (p ->> 'level')::int,
    current_xp = (p ->> 'current_xp')::int,
    lifetime_xp = (p ->> 'lifetime_xp')::int,
    strength_score = (p ->> 'strength_score')::numeric,
    physique_score = (p ->> 'physique_score')::numeric,
    endurance_score = (p ->> 'endurance_score')::numeric,
    discipline_score = (p ->> 'discipline_score')::numeric,
    hunter_score = (p ->> 'hunter_score')::numeric,
    hunter_rank = p ->> 'hunter_rank',
    current_streak_days = (p ->> 'current_streak_days')::int,
    longest_streak_days = (p ->> 'longest_streak_days')::int,
    updated_at = now()
    where user_id = v_user;

  update public.workout_sessions set progression_applied = true where id = p_session_id;
end;
$$;

-- ----------------------------------------------------------------------------
-- Claim a completed quest's reward (idempotent).
-- ----------------------------------------------------------------------------
create or replace function public.claim_quest(p_user_quest_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user      uuid := auth.uid();
  v_owner     uuid;
  v_completed boolean;
  v_claimed   boolean;
  v_reward    int;
begin
  if v_user is null then raise exception 'not authenticated'; end if;

  select uq.user_id, uq.completed, uq.claimed, q.xp_reward
    into v_owner, v_completed, v_claimed, v_reward
    from public.user_quests uq
    join public.quests q on q.id = uq.quest_id
    where uq.id = p_user_quest_id
    for update of uq;

  if v_owner is null then raise exception 'quest not found'; end if;
  if v_owner <> v_user then raise exception 'not your quest'; end if;
  if not v_completed then raise exception 'quest not completed'; end if;
  if v_claimed then return; end if; -- idempotent

  update public.user_quests set claimed = true, claimed_at = now() where id = p_user_quest_id;
  perform public.award_xp(v_user, v_reward);
end;
$$;

grant execute on function public.ensure_active_quests() to authenticated;
grant execute on function public.record_workout_for_quests(uuid) to authenticated;
grant execute on function public.apply_session_progression(uuid, jsonb) to authenticated;
grant execute on function public.claim_quest(uuid) to authenticated;

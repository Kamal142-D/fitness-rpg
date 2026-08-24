-- ============================================================================
-- Phase 10: extend complete_workout to store Gate scoring fields.
--
-- create-or-replace (functions are mutable; this supersedes the 0600 version).
-- The session now carries completion/progress/quality/gate scores, gate clear
-- rank and xp_earned; each exercise carries its performance grade + score. These
-- are computed by the pure ranking engine on the client before completion.
-- Still atomic + idempotent (client session id, ON CONFLICT DO NOTHING).
-- ============================================================================
create or replace function public.complete_workout(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user       uuid := auth.uid();
  v_session_id uuid;
  v_inserted   uuid;
  v_we_id      uuid;
  v_exercise   jsonb;
  v_set        jsonb;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  v_session_id := (payload -> 'session' ->> 'id')::uuid;
  if v_session_id is null then
    raise exception 'session id is required';
  end if;

  insert into public.workout_sessions (
    id, user_id, template_id, name, gate_difficulty, started_at, completed_at,
    duration_seconds, total_volume_kg,
    completion_score, progress_score, quality_score, gate_score, gate_clear_rank,
    xp_earned, status
  )
  values (
    v_session_id,
    v_user,
    nullif(payload -> 'session' ->> 'template_id', '')::uuid,
    payload -> 'session' ->> 'name',
    nullif(payload -> 'session' ->> 'gate_difficulty', ''),
    (payload -> 'session' ->> 'started_at')::timestamptz,
    (payload -> 'session' ->> 'completed_at')::timestamptz,
    (payload -> 'session' ->> 'duration_seconds')::int,
    (payload -> 'session' ->> 'total_volume_kg')::numeric,
    nullif(payload -> 'session' ->> 'completion_score', '')::numeric,
    nullif(payload -> 'session' ->> 'progress_score', '')::numeric,
    nullif(payload -> 'session' ->> 'quality_score', '')::numeric,
    nullif(payload -> 'session' ->> 'gate_score', '')::numeric,
    nullif(payload -> 'session' ->> 'gate_clear_rank', ''),
    coalesce((payload -> 'session' ->> 'xp_earned')::int, 0),
    'completed'
  )
  on conflict (id) do nothing
  returning id into v_inserted;

  if v_inserted is null then
    return v_session_id; -- idempotent retry
  end if;

  for v_exercise in select * from jsonb_array_elements(payload -> 'exercises')
  loop
    insert into public.workout_exercises (
      session_id, exercise_id, order_index, exercise_score, performance_grade, notes
    )
    values (
      v_session_id,
      (v_exercise ->> 'exercise_id')::uuid,
      (v_exercise ->> 'order_index')::int,
      nullif(v_exercise ->> 'exercise_score', '')::numeric,
      nullif(v_exercise ->> 'performance_grade', ''),
      v_exercise ->> 'notes'
    )
    returning id into v_we_id;

    for v_set in select * from jsonb_array_elements(v_exercise -> 'sets')
    loop
      insert into public.workout_sets (
        workout_exercise_id, set_number, weight_kg, reps, rpe,
        is_warmup, is_completed, estimated_1rm_kg, completed_at
      )
      values (
        v_we_id,
        (v_set ->> 'set_number')::int,
        nullif(v_set ->> 'weight_kg', '')::numeric,
        nullif(v_set ->> 'reps', '')::int,
        nullif(v_set ->> 'rpe', '')::numeric,
        coalesce((v_set ->> 'is_warmup')::boolean, false),
        coalesce((v_set ->> 'is_completed')::boolean, true),
        nullif(v_set ->> 'estimated_1rm_kg', '')::numeric,
        nullif(v_set ->> 'completed_at', '')::timestamptz
      );
    end loop;
  end loop;

  return v_session_id;
end;
$$;

-- ============================================================================
-- complete_workout(payload jsonb) -> uuid   (Phase 7)
--
-- Atomically writes a finished workout: the session, its exercises, and their
-- sets, in a single transaction. Idempotent via the client-supplied session id
-- (a retry with the same id returns it without duplicating). The whole function
-- is one transaction, so a mid-way failure rolls back entirely — there are no
-- partial workouts.
--
-- SECURITY DEFINER so the inserts run regardless of RLS, but ownership is pinned
-- to auth.uid() — a caller can only ever create a session for themselves.
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
    duration_seconds, total_volume_kg, status
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
    'completed'
  )
  on conflict (id) do nothing
  returning id into v_inserted;

  -- Already completed (idempotent retry): return the existing id, insert nothing.
  if v_inserted is null then
    return v_session_id;
  end if;

  for v_exercise in select * from jsonb_array_elements(payload -> 'exercises')
  loop
    insert into public.workout_exercises (session_id, exercise_id, order_index, notes)
    values (
      v_session_id,
      (v_exercise ->> 'exercise_id')::uuid,
      (v_exercise ->> 'order_index')::int,
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

grant execute on function public.complete_workout(jsonb) to authenticated;

-- ============================================================================
-- apply_workout_results (Phase 8): persist detected PRs + updated exercise stats.
--
-- PR detection itself is done in the pure TS engine (features/pr/detect.ts) —
-- this RPC only writes, atomically and idempotently. It is safe to call once per
-- completed session: a `results_applied` flag makes re-invocation a no-op.
--
-- personal_records.workout_set_id is resolved server-side from (order_index,
-- set_number), so the client never needs the persisted set ids.
-- ============================================================================

alter table public.workout_sessions
  add column if not exists results_applied boolean not null default false;

create or replace function public.apply_workout_results(
  p_session_id uuid,
  p_prs jsonb,
  p_stats jsonb
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user    uuid := auth.uid();
  v_owner   uuid;
  v_applied boolean;
  v_pr      jsonb;
  v_stat    jsonb;
  v_set_id  uuid;
begin
  if v_user is null then
    raise exception 'not authenticated';
  end if;

  select user_id, results_applied into v_owner, v_applied
  from public.workout_sessions
  where id = p_session_id
  for update;

  if v_owner is null then
    raise exception 'session not found';
  end if;
  if v_owner <> v_user then
    raise exception 'not your session';
  end if;
  if v_applied then
    return; -- idempotent: results already applied
  end if;

  for v_pr in select * from jsonb_array_elements(coalesce(p_prs, '[]'::jsonb))
  loop
    select ws.id into v_set_id
    from public.workout_exercises we
    join public.workout_sets ws on ws.workout_exercise_id = we.id
    where we.session_id = p_session_id
      and we.order_index = (v_pr ->> 'order_index')::int
      and ws.set_number = (v_pr ->> 'set_number')::int
    limit 1;

    insert into public.personal_records (
      user_id, exercise_id, workout_set_id, record_type, previous_value, new_value
    )
    values (
      v_user,
      (v_pr ->> 'exercise_id')::uuid,
      v_set_id,
      v_pr ->> 'record_type',
      nullif(v_pr ->> 'previous_value', '')::numeric,
      (v_pr ->> 'new_value')::numeric
    );
  end loop;

  for v_stat in select * from jsonb_array_elements(coalesce(p_stats, '[]'::jsonb))
  loop
    insert into public.exercise_user_stats (
      user_id, exercise_id, best_weight_kg, best_reps, best_estimated_1rm_kg,
      best_volume_kg, total_sessions, last_performed_at, updated_at
    )
    values (
      v_user,
      (v_stat ->> 'exercise_id')::uuid,
      nullif(v_stat ->> 'best_weight_kg', '')::numeric,
      nullif(v_stat ->> 'best_reps', '')::int,
      nullif(v_stat ->> 'best_estimated_1rm_kg', '')::numeric,
      nullif(v_stat ->> 'best_volume_kg', '')::numeric,
      1,
      now(),
      now()
    )
    on conflict (user_id, exercise_id) do update set
      best_weight_kg = greatest(
        coalesce(public.exercise_user_stats.best_weight_kg, 0),
        coalesce(excluded.best_weight_kg, 0)
      ),
      best_reps = greatest(
        coalesce(public.exercise_user_stats.best_reps, 0),
        coalesce(excluded.best_reps, 0)
      ),
      best_estimated_1rm_kg = greatest(
        coalesce(public.exercise_user_stats.best_estimated_1rm_kg, 0),
        coalesce(excluded.best_estimated_1rm_kg, 0)
      ),
      best_volume_kg = greatest(
        coalesce(public.exercise_user_stats.best_volume_kg, 0),
        coalesce(excluded.best_volume_kg, 0)
      ),
      total_sessions = public.exercise_user_stats.total_sessions + 1,
      last_performed_at = now(),
      updated_at = now();
  end loop;

  update public.workout_sessions set results_applied = true where id = p_session_id;
end;
$$;

grant execute on function public.apply_workout_results(uuid, jsonb, jsonb) to authenticated;

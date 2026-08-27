-- Ranking System V3: one nine-band ladder, RP within each band, evidence-aware
-- exercise ranking, and explicit separation of Gate difficulty / clear grade.
-- Raw sets, sessions, PRs, XP and historical ranks are preserved.

-- Expand every persisted rank column before V3 values can be written.
alter table public.workout_sessions drop constraint if exists workout_sessions_gate_difficulty_check;
alter table public.workout_sessions add constraint workout_sessions_gate_difficulty_check
  check (gate_difficulty in ('E','D','C','B','A','S','S+','SS','SSS'));
alter table public.workout_sessions drop constraint if exists workout_sessions_gate_clear_rank_check;
alter table public.workout_sessions add constraint workout_sessions_gate_clear_rank_check
  check (gate_clear_rank in ('E','D','C','B','A','S','S+','SS','SSS'));
alter table public.workout_sessions drop constraint if exists workout_sessions_gate_difficulty_rank_check;
alter table public.workout_sessions add constraint workout_sessions_gate_difficulty_rank_check
  check (gate_difficulty_rank in ('E','D','C','B','A','S','S+','SS','SSS'));

alter table public.workout_exercises drop constraint if exists workout_exercises_exercise_rank_at_time_check;
alter table public.workout_exercises add constraint workout_exercises_exercise_rank_at_time_check
  check (exercise_rank_at_time in ('E','D','C','B','A','S','S+','SS','SSS'));
alter table public.workout_exercises drop constraint if exists workout_exercises_difficulty_rank_check;
alter table public.workout_exercises add constraint workout_exercises_difficulty_rank_check
  check (difficulty_rank in ('E','D','C','B','A','S','S+','SS','SSS'));

alter table public.exercise_user_stats drop constraint if exists exercise_user_stats_exercise_rank_check;
alter table public.exercise_user_stats add constraint exercise_user_stats_exercise_rank_check
  check (exercise_rank in ('E','D','C','B','A','S','S+','SS','SSS'));

alter table public.player_progression drop constraint if exists player_progression_hunter_rank_check;
alter table public.player_progression add constraint player_progression_hunter_rank_check
  check (hunter_rank in ('E','D','C','B','A','S','S+','SS','SSS'));
alter table public.player_progression drop constraint if exists player_progression_hunter_rank_cap_check;
alter table public.player_progression add constraint player_progression_hunter_rank_cap_check
  check (hunter_rank_cap in ('E','D','C','B','A','S','S+','SS','SSS'));

alter table public.workout_templates drop constraint if exists workout_templates_last_difficulty_rank_check;
alter table public.workout_templates add constraint workout_templates_last_difficulty_rank_check
  check (last_difficulty_rank in ('E','D','C','B','A','S','S+','SS','SSS'));
alter table public.workout_templates drop constraint if exists workout_templates_average_difficulty_rank_check;
alter table public.workout_templates add constraint workout_templates_average_difficulty_rank_check
  check (average_difficulty_rank in ('E','D','C','B','A','S','S+','SS','SSS'));

alter table public.player_progression
  alter column rank_system_version set default 3,
  add column if not exists hunter_rp integer not null default 0 check (hunter_rp between 0 and 100),
  add column if not exists physique_rp integer not null default 0 check (physique_rp between 0 and 100),
  add column if not exists strength_rp integer not null default 0 check (strength_rp between 0 and 100),
  add column if not exists conditioning_rp integer not null default 0 check (conditioning_rp between 0 and 100);

-- V3 does not reset anyone. It marks the formula version while retaining source
-- evidence and the last displayed result until the native engine recalculates it.
update public.player_progression set rank_system_version = 3;

alter table public.workout_exercises
  add column if not exists ranking_mode text check (ranking_mode in ('global','personal','unranked')),
  add column if not exists exercise_rp integer check (exercise_rp between 0 and 100),
  add column if not exists exercise_rp_delta integer check (exercise_rp_delta >= 0),
  add column if not exists baseline_session_count integer check (baseline_session_count >= 0),
  add column if not exists today_performance text check (today_performance in ('Baseline','Below Baseline','Normal','Strong','Excellent','PR'));

alter table public.exercise_user_stats
  add column if not exists ranking_mode text check (ranking_mode in ('global','personal','unranked')),
  add column if not exists rank_rp integer not null default 0 check (rank_rp between 0 and 100),
  add column if not exists baseline_performance numeric(8,3) check (baseline_performance > 0),
  add column if not exists validation_count integer not null default 0 check (validation_count >= 0);

comment on column public.workout_exercises.performance_grade is
  'Legacy V1/V2 temporary letter grade. V3 clients leave this null and use today_performance text.';
comment on column public.workout_exercises.exercise_rank_at_time is
  'Permanent Global Strength Rank or Personal Tier at workout completion.';

-- One row per completed session/exercise, ordered by performed_at by clients.
-- This gives personal movements real session evidence without using lifetime PRs.
create or replace view public.exercise_performance_history_v3
with (security_invoker = true)
as
select
  s.user_id,
  s.id as session_id,
  we.exercise_id,
  max(ws.estimated_1rm_kg)::numeric(8,3) as session_best_1rm_kg,
  coalesce(s.completed_at, max(ws.completed_at), s.started_at) as performed_at
from public.workout_sessions s
join public.workout_exercises we on we.session_id = s.id
join public.workout_sets ws on ws.workout_exercise_id = we.id
where s.status = 'completed'
  and ws.is_completed
  and not ws.is_warmup
  and ws.estimated_1rm_kg is not null
  and ws.estimated_1rm_kg > 0
group by s.user_id, s.id, we.exercise_id, s.completed_at, s.started_at;

grant select on public.exercise_performance_history_v3 to authenticated;

create or replace function public.complete_workout(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid(); v_session_id uuid; v_inserted uuid; v_we_id uuid;
  v_exercise jsonb; v_set jsonb; v_template_id uuid;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  v_session_id := (payload -> 'session' ->> 'id')::uuid;
  if v_session_id is null then raise exception 'session id is required'; end if;
  v_template_id := nullif(payload -> 'session' ->> 'template_id', '')::uuid;

  insert into public.workout_sessions (
    id,user_id,template_id,name,gate_difficulty,gate_difficulty_score,gate_difficulty_rank,
    started_at,completed_at,duration_seconds,total_volume_kg,completion_score,progress_score,
    quality_score,gate_score,gate_clear_rank,xp_earned,status
  ) values (
    v_session_id,v_user,v_template_id,payload->'session'->>'name',
    nullif(payload->'session'->>'gate_difficulty_rank',''),
    nullif(payload->'session'->>'gate_difficulty_score','')::numeric,
    nullif(payload->'session'->>'gate_difficulty_rank',''),
    (payload->'session'->>'started_at')::timestamptz,(payload->'session'->>'completed_at')::timestamptz,
    (payload->'session'->>'duration_seconds')::int,(payload->'session'->>'total_volume_kg')::numeric,
    nullif(payload->'session'->>'completion_score','')::numeric,
    nullif(payload->'session'->>'progress_score','')::numeric,
    nullif(payload->'session'->>'quality_score','')::numeric,
    nullif(payload->'session'->>'gate_score','')::numeric,
    nullif(payload->'session'->>'gate_clear_rank',''),coalesce((payload->'session'->>'xp_earned')::int,0),'completed'
  ) on conflict (id) do nothing returning id into v_inserted;
  if v_inserted is null then return v_session_id; end if;

  for v_exercise in select * from jsonb_array_elements(payload->'exercises') loop
    insert into public.workout_exercises (
      session_id,exercise_id,order_index,exercise_score,exercise_rank_at_time,performance_grade,
      difficulty_score,difficulty_rank,ranking_mode,exercise_rp,exercise_rp_delta,
      baseline_session_count,today_performance,notes
    ) values (
      v_session_id,(v_exercise->>'exercise_id')::uuid,(v_exercise->>'order_index')::int,
      nullif(v_exercise->>'exercise_score','')::numeric,
      nullif(v_exercise->>'exercise_rank_at_time',''),null,
      nullif(v_exercise->>'difficulty_score','')::numeric,nullif(v_exercise->>'difficulty_rank',''),
      nullif(v_exercise->>'ranking_mode',''),nullif(v_exercise->>'exercise_rp','')::int,
      nullif(v_exercise->>'exercise_rp_delta','')::int,nullif(v_exercise->>'baseline_session_count','')::int,
      nullif(v_exercise->>'today_performance',''),v_exercise->>'notes'
    ) returning id into v_we_id;
    for v_set in select * from jsonb_array_elements(v_exercise->'sets') loop
      insert into public.workout_sets (
        workout_exercise_id,set_number,weight_kg,reps,rpe,is_warmup,is_completed,estimated_1rm_kg,completed_at
      ) values (
        v_we_id,(v_set->>'set_number')::int,nullif(v_set->>'weight_kg','')::numeric,
        nullif(v_set->>'reps','')::int,nullif(v_set->>'rpe','')::numeric,
        coalesce((v_set->>'is_warmup')::boolean,false),coalesce((v_set->>'is_completed')::boolean,true),
        nullif(v_set->>'estimated_1rm_kg','')::numeric,nullif(v_set->>'completed_at','')::timestamptz
      );
    end loop;
  end loop;

  if v_template_id is not null then
    update public.workout_templates t set
      last_difficulty_score = nullif(payload->'session'->>'gate_difficulty_score','')::numeric,
      last_difficulty_rank = nullif(payload->'session'->>'gate_difficulty_rank',''),
      average_difficulty_score = ((coalesce(t.average_difficulty_score,0) * t.times_completed) +
        nullif(payload->'session'->>'gate_difficulty_score','')::numeric) / (t.times_completed + 1),
      times_completed = t.times_completed + 1,
      last_completed_at = (payload->'session'->>'completed_at')::timestamptz
    where t.id = v_template_id;
    update public.workout_templates set average_difficulty_rank = case
      when average_difficulty_score < 10 then 'E'
      when average_difficulty_score < 25 then 'D'
      when average_difficulty_score < 50 then 'C'
      when average_difficulty_score < 70 then 'B'
      when average_difficulty_score < 85 then 'A'
      when average_difficulty_score < 93 then 'S'
      when average_difficulty_score < 97 then 'S+'
      when average_difficulty_score < 99 then 'SS'
      else 'SSS' end where id = v_template_id;
  end if;
  return v_session_id;
end;
$$;


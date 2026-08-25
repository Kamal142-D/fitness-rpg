-- Personalized post-workout Gate Difficulty, rich exercise metadata, and safe
-- routine archival. Existing sessions and templates are preserved.

alter table public.exercises
  add column if not exists aliases text[] not null default '{}',
  add column if not exists body_part text,
  add column if not exists target_muscle text,
  add column if not exists instructions text[] not null default '{}',
  add column if not exists image_url text,
  add column if not exists video_url text,
  add column if not exists source text,
  add column if not exists source_id text,
  add column if not exists attribution text;

-- The source contains a handful of same-name records. Identity is source + ID,
-- not display name, because equipment/variation must remain distinguishable.
alter table public.exercises drop constraint if exists exercises_name_key;
create unique index if not exists exercises_source_identity_uidx
  on public.exercises (source, source_id) where source is not null and source_id is not null;
create index if not exists exercises_search_name_idx on public.exercises using gin (to_tsvector('simple', name));
create index if not exists exercises_category_idx on public.exercises (category);
create index if not exists exercises_equipment_idx on public.exercises (equipment);

alter table public.workout_templates
  add column if not exists deleted_at timestamptz,
  add column if not exists last_difficulty_score numeric(5,2) check (last_difficulty_score between 0 and 100),
  add column if not exists last_difficulty_rank text check (last_difficulty_rank in ('E','D','C','B','A','S')),
  add column if not exists average_difficulty_score numeric(5,2) check (average_difficulty_score between 0 and 100),
  add column if not exists average_difficulty_rank text check (average_difficulty_rank in ('E','D','C','B','A','S')),
  add column if not exists times_completed integer not null default 0 check (times_completed >= 0),
  add column if not exists last_completed_at timestamptz;

-- Legacy planned difficulty remains nullable for backward compatibility, but
-- native clients no longer set or display it.
comment on column public.workout_templates.difficulty is
  'Legacy planned difficulty. New clients leave this null; actual difficulty is session-derived.';

alter table public.workout_sessions
  add column if not exists gate_difficulty_score numeric(5,2) check (gate_difficulty_score between 0 and 100),
  add column if not exists gate_difficulty_rank text check (gate_difficulty_rank in ('E','D','C','B','A','S'));

alter table public.workout_exercises
  add column if not exists difficulty_score numeric(5,2) check (difficulty_score between 0 and 100),
  add column if not exists difficulty_rank text check (difficulty_rank in ('E','D','C','B','A','S'));

create table if not exists public.hidden_system_templates (
  user_id uuid not null references auth.users(id) on delete cascade,
  template_id uuid not null references public.workout_templates(id) on delete cascade,
  hidden_at timestamptz not null default now(),
  primary key (user_id, template_id)
);
alter table public.hidden_system_templates enable row level security;
drop policy if exists "hidden_system_templates_all_own" on public.hidden_system_templates;
create policy "hidden_system_templates_all_own" on public.hidden_system_templates
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

create or replace function public.archive_workout_template(p_template_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.workout_templates
  set deleted_at = now()
  where id = p_template_id
    and user_id = auth.uid()
    and not is_system_template;
  if not found then raise exception 'routine not found or cannot be deleted'; end if;
end;
$$;

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
      session_id,exercise_id,order_index,exercise_score,performance_grade,difficulty_score,difficulty_rank,notes
    ) values (
      v_session_id,(v_exercise->>'exercise_id')::uuid,(v_exercise->>'order_index')::int,
      nullif(v_exercise->>'exercise_score','')::numeric,nullif(v_exercise->>'performance_grade',''),
      nullif(v_exercise->>'difficulty_score','')::numeric,nullif(v_exercise->>'difficulty_rank',''),v_exercise->>'notes'
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
      when average_difficulty_score < 20 then 'E' when average_difficulty_score < 35 then 'D'
      when average_difficulty_score < 50 then 'C' when average_difficulty_score < 65 then 'B'
      when average_difficulty_score < 80 then 'A' else 'S' end where id = v_template_id;
  end if;
  return v_session_id;
end;
$$;

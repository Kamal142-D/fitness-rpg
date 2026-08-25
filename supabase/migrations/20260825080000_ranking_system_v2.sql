-- Ranking System V2. Derived ranks are reset conservatively; XP, level, sessions,
-- PRs, exercise history, quests and all source workout data are preserved.
alter table public.body_assessments
  add column if not exists waist_cm numeric(6,2) check (waist_cm between 40 and 200),
  add column if not exists muscle_mass_kg numeric(6,3) check (muscle_mass_kg >= 0),
  add column if not exists lean_body_mass_kg numeric(6,3) check (lean_body_mass_kg >= 0),
  add column if not exists left_arm_lean_mass_kg numeric(6,3) check (left_arm_lean_mass_kg >= 0),
  add column if not exists right_arm_lean_mass_kg numeric(6,3) check (right_arm_lean_mass_kg >= 0),
  add column if not exists left_leg_lean_mass_kg numeric(6,3) check (left_leg_lean_mass_kg >= 0),
  add column if not exists right_leg_lean_mass_kg numeric(6,3) check (right_leg_lean_mass_kg >= 0);

alter table public.body_assessments drop constraint if exists body_assessments_source_check;
alter table public.body_assessments add constraint body_assessments_source_check
  check (source in ('manual', 'inbody', 'smart_scale', 'other'));

create table if not exists public.strength_assessment_sets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  exercise_id text not null,
  variation text not null default 'standard',
  equipment text not null check (equipment in ('barbell','dumbbell','smith_machine','machine','cable','bodyweight','other')),
  weight_kg numeric(7,2) not null check (weight_kg >= 0),
  reps integer not null check (reps between 1 and 50),
  weight_mode text check (weight_mode in ('total','per_hand')),
  rpe numeric(3,1) check (rpe between 1 and 10),
  assessed_at timestamptz not null default now()
);

create table if not exists public.conditioning_assessments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  test_type text not null check (test_type in ('cooper_12_minute','run_1_5_mile','step_3_minute')),
  result numeric(9,2) not null check (result > 0),
  score numeric(5,2) check (score between 0 and 100),
  assessed_at timestamptz not null default now()
);

alter table public.player_progression
  add column if not exists rank_system_version integer not null default 2,
  add column if not exists hunter_rank_provisional boolean not null default true,
  add column if not exists hunter_rank_confidence text not null default 'low' check (hunter_rank_confidence in ('low','medium','high')),
  add column if not exists hunter_rank_cap text check (hunter_rank_cap in ('E','D','C','B','A','S')),
  add column if not exists hunter_rank_reasons jsonb not null default '[]'::jsonb,
  add column if not exists assessment_update_required boolean not null default true,
  add column if not exists conditioning_score numeric(5,2) check (conditioning_score between 0 and 100);

create index if not exists strength_assessment_sets_user_date_idx
  on public.strength_assessment_sets(user_id, assessed_at desc);
create index if not exists conditioning_assessments_user_date_idx
  on public.conditioning_assessments(user_id, assessed_at desc);

-- Current benchmarkable strength evidence. Lifetime PR rollups are intentionally
-- excluded; the Android rank engine applies the 60-day validity window.
create or replace view public.strength_evidence_v2
with (security_invoker = true)
as
select
  s.user_id,
  s.id as session_id,
  e.name as exercise_name,
  lower(coalesce(e.equipment, 'other')) as equipment,
  case
    when lower(e.name) like '%front squat%' then 'front'
    when lower(e.name) like '%squat%' then 'back'
    when lower(e.name) like '%bench%' then 'flat'
    when lower(e.name) like '%deadlift%' then 'conventional'
    when lower(e.name) like '%overhead%' then 'standing'
    when lower(e.name) like '%pull%up%' then 'strict'
    else 'standard'
  end as variation,
  ws.weight_kg,
  ws.reps,
  ws.rpe,
  coalesce(ws.completed_at, s.completed_at) as performed_at
from public.workout_sessions s
join public.workout_exercises we on we.session_id = s.id
join public.exercises e on e.id = we.exercise_id
join public.workout_sets ws on ws.workout_exercise_id = we.id
where s.status = 'completed'
  and ws.is_completed
  and not ws.is_warmup
  and ws.weight_kg is not null
  and ws.reps between 1 and 50
  and lower(coalesce(e.equipment, '')) in ('barbell', 'bodyweight', 'body weight');

-- Personalized Gate baselines, one row per user/exercise over recent sessions.
create or replace view public.exercise_recent_baselines_v2
with (security_invoker = true)
as
select
  x.user_id,
  x.exercise_id,
  count(*)::integer as session_count,
  avg(x.session_volume_kg)::numeric(10,3) as recent_average_volume_kg,
  max(x.session_best_1rm_kg)::numeric(7,3) as recent_best_1rm_kg
from (
  select
    s.user_id,
    s.id as session_id,
    we.exercise_id,
    sum(coalesce(ws.weight_kg,0) * coalesce(ws.reps,0)) as session_volume_kg,
    max(ws.estimated_1rm_kg) as session_best_1rm_kg
  from public.workout_sessions s
  join public.workout_exercises we on we.session_id = s.id
  join public.workout_sets ws on ws.workout_exercise_id = we.id
  where s.status = 'completed'
    and s.completed_at >= now() - interval '60 days'
    and ws.is_completed
    and not ws.is_warmup
  group by s.user_id, s.id, we.exercise_id
) x
group by x.user_id, x.exercise_id;

grant select on public.strength_evidence_v2, public.exercise_recent_baselines_v2 to authenticated;

update public.player_progression set
  hunter_score = 0, hunter_rank = 'E', strength_score = 0, conditioning_score = null,
  physique_score = 0, endurance_score = 0,
  hunter_rank_provisional = true, hunter_rank_confidence = 'low',
  hunter_rank_cap = 'C', assessment_update_required = true,
  hunter_rank_reasons = '["Ranking System V2 requires updated physical assessment data."]'::jsonb,
  rank_system_version = 2;

alter table public.strength_assessment_sets enable row level security;
alter table public.conditioning_assessments enable row level security;
drop policy if exists "strength_assessments_own" on public.strength_assessment_sets;
drop policy if exists "conditioning_assessments_own" on public.conditioning_assessments;
create policy "strength_assessments_own" on public.strength_assessment_sets for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "conditioning_assessments_own" on public.conditioning_assessments for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

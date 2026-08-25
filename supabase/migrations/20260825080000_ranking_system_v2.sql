-- Ranking System V2. Derived ranks are reset conservatively; XP, level, sessions,
-- PRs, exercise history, quests and all source workout data are preserved.
alter table public.body_assessments
  add column if not exists waist_cm numeric(6,2) check (waist_cm between 40 and 200),
  add column if not exists muscle_mass_kg numeric(6,3) check (muscle_mass_kg >= 0),
  add column if not exists lean_body_mass_kg numeric(6,3) check (lean_body_mass_kg >= 0);

alter table public.body_assessments drop constraint if exists body_assessments_source_check;
alter table public.body_assessments add constraint body_assessments_source_check
  check (source in ('manual', 'inbody', 'smart_scale', 'other'));

create table if not exists public.strength_assessment_sets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  exercise_id text not null,
  variation text not null default 'standard',
  equipment text not null check (equipment in ('barbell','dumbbell','smith_machine','machine','cable','bodyweight','other')),
  weight_kg numeric(7,2) not null check (weight_kg > 0),
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
  add column if not exists assessment_update_required boolean not null default true;

update public.player_progression set
  hunter_score = 0, hunter_rank = 'E', strength_score = 0,
  physique_score = 0, endurance_score = 0,
  hunter_rank_provisional = true, hunter_rank_confidence = 'low',
  hunter_rank_cap = 'C', assessment_update_required = true,
  hunter_rank_reasons = '["Ranking System V2 requires updated physical assessment data."]'::jsonb,
  rank_system_version = 2;

alter table public.strength_assessment_sets enable row level security;
alter table public.conditioning_assessments enable row level security;
create policy "strength_assessments_own" on public.strength_assessment_sets for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "conditioning_assessments_own" on public.conditioning_assessments for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

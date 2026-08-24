-- ============================================================================
-- Initial schema (PLAN.txt §5)
--
-- Conventions:
--   * UUID primary keys (gen_random_uuid(), core since PG13)
--   * timestamptz timestamps, created_at/updated_at where useful
--   * Canonical weights stored in KILOGRAMS
--   * Rank letters (E,D,C,B,A,S) and other small domains validated with CHECKs
--   * Foreign keys with deliberate ON DELETE behavior
--   * RLS is enabled and policed in the next migration (kept separate for review)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Shared helper: keep updated_at fresh on UPDATE.
-- ----------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ----------------------------------------------------------------------------
-- profiles — one row per auth user (basic profile + onboarding answers).
-- ----------------------------------------------------------------------------
create table public.profiles (
  id                        uuid primary key references auth.users (id) on delete cascade,
  email                     text,
  display_name              text,
  date_of_birth             date,
  sex                       text check (sex in ('male', 'female', 'intersex', 'prefer_not_to_say')),
  height_cm                 numeric(5, 2) check (height_cm > 0 and height_cm < 300),
  current_weight_kg         numeric(6, 3) check (current_weight_kg > 0 and current_weight_kg < 700),
  experience_level          text check (experience_level in ('beginner', 'intermediate', 'advanced')),
  fitness_goal              text check (fitness_goal in ('build_muscle', 'lose_fat', 'get_stronger', 'general_fitness', 'improve_endurance')),
  training_days_per_week    smallint check (training_days_per_week between 0 and 7),
  training_location         text check (training_location in ('gym', 'home')),
  preferred_workout_minutes smallint check (preferred_workout_minutes > 0 and preferred_workout_minutes <= 360),
  onboarding_completed      boolean not null default false,
  created_at                timestamptz not null default now(),
  updated_at                timestamptz not null default now()
);
comment on table public.profiles is 'Per-user profile and onboarding answers. id = auth.users.id.';

-- ----------------------------------------------------------------------------
-- body_assessments — manual or InBody body-composition snapshots.
-- ----------------------------------------------------------------------------
create table public.body_assessments (
  id                      uuid primary key default gen_random_uuid(),
  user_id                 uuid not null references auth.users (id) on delete cascade,
  weight_kg               numeric(6, 3) check (weight_kg > 0 and weight_kg < 700),
  body_fat_percent        numeric(5, 2) check (body_fat_percent >= 0 and body_fat_percent <= 100),
  skeletal_muscle_mass_kg numeric(6, 3) check (skeletal_muscle_mass_kg >= 0),
  body_fat_mass_kg        numeric(6, 3) check (body_fat_mass_kg >= 0),
  visceral_fat_level      numeric(5, 2) check (visceral_fat_level >= 0),
  inbody_score            numeric(6, 2),
  assessment_date         date not null default current_date,
  source                  text not null default 'manual' check (source in ('manual', 'inbody', 'other')),
  source_file_path        text,
  created_at              timestamptz not null default now()
);
comment on table public.body_assessments is 'User body-composition assessments (manual entry or InBody upload).';

-- ----------------------------------------------------------------------------
-- exercises — global reference catalog (system-owned, read-only to users).
-- ----------------------------------------------------------------------------
create table public.exercises (
  id                      uuid primary key default gen_random_uuid(),
  name                    text not null unique,
  category                text not null check (category in ('chest', 'back', 'legs', 'shoulders', 'arms', 'core', 'full_body', 'cardio')),
  primary_muscle_group    text,
  secondary_muscle_groups text[] not null default '{}',
  equipment               text,
  exercise_type           text not null check (exercise_type in ('strength', 'bodyweight', 'cardio', 'mobility')),
  ranking_enabled         boolean not null default true,
  created_at              timestamptz not null default now()
);
comment on table public.exercises is 'Global exercise catalog. Readable by all authenticated users; writable only by trusted server/admin roles.';

-- ----------------------------------------------------------------------------
-- workout_templates — "Gates". System templates (user_id null) or user-made.
-- ----------------------------------------------------------------------------
create table public.workout_templates (
  id                         uuid primary key default gen_random_uuid(),
  user_id                    uuid references auth.users (id) on delete cascade,
  name                       text not null,
  description                text,
  estimated_duration_minutes smallint check (estimated_duration_minutes > 0 and estimated_duration_minutes <= 360),
  difficulty                 text check (difficulty in ('E', 'D', 'C', 'B', 'A', 'S')),
  is_system_template         boolean not null default false,
  created_at                 timestamptz not null default now(),
  updated_at                 timestamptz not null default now(),
  -- A system template has no owner; a user template must have one.
  constraint workout_templates_ownership_ck check (
    (is_system_template and user_id is null) or (not is_system_template and user_id is not null)
  )
);
comment on table public.workout_templates is 'Workout templates presented as Gates. difficulty here is the intended Gate Difficulty, not a Gate Clear Rank.';

-- ----------------------------------------------------------------------------
-- workout_template_exercises — ordered exercises + targets within a template.
-- ----------------------------------------------------------------------------
create table public.workout_template_exercises (
  id              uuid primary key default gen_random_uuid(),
  template_id     uuid not null references public.workout_templates (id) on delete cascade,
  exercise_id     uuid not null references public.exercises (id) on delete restrict,
  order_index     smallint not null,
  target_sets     smallint check (target_sets > 0),
  target_reps_min smallint check (target_reps_min >= 0),
  target_reps_max smallint check (target_reps_max >= 0),
  target_rpe      numeric(3, 1) check (target_rpe >= 0 and target_rpe <= 10),
  rest_seconds    smallint check (rest_seconds >= 0),
  constraint wte_reps_order_ck check (target_reps_max is null or target_reps_min is null or target_reps_max >= target_reps_min),
  unique (template_id, order_index)
);

-- ----------------------------------------------------------------------------
-- workout_sessions — a single training session (an attempt at a Gate).
-- ----------------------------------------------------------------------------
create table public.workout_sessions (
  id               uuid primary key default gen_random_uuid(),
  user_id          uuid not null references auth.users (id) on delete cascade,
  template_id      uuid references public.workout_templates (id) on delete set null,
  name             text,
  gate_difficulty  text check (gate_difficulty in ('E', 'D', 'C', 'B', 'A', 'S')),
  started_at       timestamptz not null default now(),
  completed_at     timestamptz,
  duration_seconds integer check (duration_seconds >= 0),
  total_volume_kg  numeric(10, 3) check (total_volume_kg >= 0),
  completion_score numeric(5, 2) check (completion_score between 0 and 100),
  progress_score   numeric(5, 2) check (progress_score between 0 and 100),
  quality_score    numeric(5, 2) check (quality_score between 0 and 100),
  gate_score       numeric(5, 2) check (gate_score between 0 and 100),
  gate_clear_rank  text check (gate_clear_rank in ('E', 'D', 'C', 'B', 'A', 'S')),
  xp_earned        integer not null default 0 check (xp_earned >= 0),
  status           text not null default 'active' check (status in ('active', 'completed', 'abandoned')),
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);
comment on table public.workout_sessions is 'One workout attempt. gate_difficulty is chosen before; gate_clear_rank/gate_score are computed after.';

-- ----------------------------------------------------------------------------
-- workout_exercises — an exercise performed within a session.
-- ----------------------------------------------------------------------------
create table public.workout_exercises (
  id                   uuid primary key default gen_random_uuid(),
  session_id           uuid not null references public.workout_sessions (id) on delete cascade,
  exercise_id          uuid not null references public.exercises (id) on delete restrict,
  order_index          smallint not null,
  exercise_score       numeric(5, 2) check (exercise_score between 0 and 100),
  exercise_rank_at_time text check (exercise_rank_at_time in ('E', 'D', 'C', 'B', 'A', 'S')),
  performance_grade    text check (performance_grade in ('E', 'D', 'C', 'B', 'A', 'S')),
  notes                text,
  unique (session_id, order_index)
);
comment on table public.workout_exercises is 'Exercise instance in a session. exercise_rank_at_time (permanent) and performance_grade (today) are distinct concepts.';

-- ----------------------------------------------------------------------------
-- workout_sets — individual sets. Raw logs are preserved even if excluded
-- from ranking (validation happens in the engine, not by deleting rows).
-- ----------------------------------------------------------------------------
create table public.workout_sets (
  id                  uuid primary key default gen_random_uuid(),
  workout_exercise_id uuid not null references public.workout_exercises (id) on delete cascade,
  set_number          smallint not null,
  weight_kg           numeric(7, 3) check (weight_kg >= 0),
  reps                smallint check (reps >= 0),
  rpe                 numeric(3, 1) check (rpe >= 0 and rpe <= 10),
  duration_seconds    integer check (duration_seconds >= 0),
  distance_meters     numeric(9, 2) check (distance_meters >= 0),
  is_warmup           boolean not null default false,
  is_completed        boolean not null default false,
  is_pr               boolean not null default false,
  estimated_1rm_kg    numeric(7, 3) check (estimated_1rm_kg >= 0),
  completed_at        timestamptz,
  unique (workout_exercise_id, set_number)
);

-- ----------------------------------------------------------------------------
-- exercise_user_stats — rolled-up per-user, per-exercise bests + rank.
-- ----------------------------------------------------------------------------
create table public.exercise_user_stats (
  user_id                  uuid not null references auth.users (id) on delete cascade,
  exercise_id              uuid not null references public.exercises (id) on delete cascade,
  best_weight_kg           numeric(7, 3) check (best_weight_kg >= 0),
  best_reps                smallint check (best_reps >= 0),
  best_estimated_1rm_kg    numeric(7, 3) check (best_estimated_1rm_kg >= 0),
  best_volume_kg           numeric(10, 3) check (best_volume_kg >= 0),
  rank_score               numeric(5, 2) check (rank_score between 0 and 100),
  exercise_rank            text check (exercise_rank in ('E', 'D', 'C', 'B', 'A', 'S')),
  qualifying_session_count integer not null default 0 check (qualifying_session_count >= 0),
  total_sessions           integer not null default 0 check (total_sessions >= 0),
  last_performed_at        timestamptz,
  updated_at               timestamptz not null default now(),
  primary key (user_id, exercise_id)
);

-- ----------------------------------------------------------------------------
-- player_progression — one row per user: level, XP, attributes, Hunter rank.
-- ----------------------------------------------------------------------------
create table public.player_progression (
  user_id             uuid primary key references auth.users (id) on delete cascade,
  level               integer not null default 1 check (level >= 1),
  current_xp          integer not null default 0 check (current_xp >= 0),
  lifetime_xp         integer not null default 0 check (lifetime_xp >= 0),
  hunter_score        numeric(5, 2) not null default 0 check (hunter_score between 0 and 100),
  hunter_rank         text not null default 'E' check (hunter_rank in ('E', 'D', 'C', 'B', 'A', 'S')),
  strength_score      numeric(5, 2) not null default 0 check (strength_score between 0 and 100),
  physique_score      numeric(5, 2) not null default 0 check (physique_score between 0 and 100),
  endurance_score     numeric(5, 2) not null default 0 check (endurance_score between 0 and 100),
  discipline_score    numeric(5, 2) not null default 0 check (discipline_score between 0 and 100),
  current_streak_days integer not null default 0 check (current_streak_days >= 0),
  longest_streak_days integer not null default 0 check (longest_streak_days >= 0),
  updated_at          timestamptz not null default now()
);
comment on table public.player_progression is 'Level = activity/account progression; Hunter rank = demonstrated performance. They are distinct.';

-- ----------------------------------------------------------------------------
-- quests — global quest definitions (read-only to users).
-- ----------------------------------------------------------------------------
create table public.quests (
  id               uuid primary key default gen_random_uuid(),
  name             text not null,
  description      text,
  type             text not null check (type in ('daily', 'weekly', 'achievement', 'special')),
  requirement_type text not null,
  requirement_value numeric not null check (requirement_value >= 0),
  xp_reward        integer not null default 0 check (xp_reward >= 0),
  gold_reward      integer not null default 0 check (gold_reward >= 0),
  active           boolean not null default true,
  created_at       timestamptz not null default now()
);
comment on table public.quests is 'Global quest definitions. Active ones are readable by authenticated users; writable only by trusted roles.';

-- ----------------------------------------------------------------------------
-- user_quests — a quest assigned to a user, with progress/claim state.
-- ----------------------------------------------------------------------------
create table public.user_quests (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users (id) on delete cascade,
  quest_id     uuid not null references public.quests (id) on delete cascade,
  progress     numeric not null default 0 check (progress >= 0),
  completed    boolean not null default false,
  claimed      boolean not null default false,
  assigned_at  timestamptz not null default now(),
  expires_at   timestamptz,
  completed_at timestamptz,
  claimed_at   timestamptz,
  -- Cannot claim what is not completed (reward-claim integrity).
  constraint user_quests_claim_requires_complete_ck check (not claimed or completed)
);

-- ----------------------------------------------------------------------------
-- personal_records — detected PRs (weight, reps, estimated 1RM, volume).
-- ----------------------------------------------------------------------------
create table public.personal_records (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users (id) on delete cascade,
  exercise_id    uuid not null references public.exercises (id) on delete cascade,
  workout_set_id uuid references public.workout_sets (id) on delete set null,
  record_type    text not null check (record_type in ('weight', 'reps', 'estimated_1rm', 'volume')),
  previous_value numeric,
  new_value      numeric not null,
  achieved_at    timestamptz not null default now()
);

-- ============================================================================
-- Indexes (PLAN.txt §5)
-- ============================================================================
create index idx_body_assessments_user_date on public.body_assessments (user_id, assessment_date desc);
create index idx_workout_templates_user on public.workout_templates (user_id);
create index idx_workout_templates_system on public.workout_templates (is_system_template) where is_system_template;
create index idx_wte_template on public.workout_template_exercises (template_id, order_index);
create index idx_wte_exercise on public.workout_template_exercises (exercise_id);
create index idx_workout_sessions_user_started on public.workout_sessions (user_id, started_at desc);
create index idx_workout_sessions_template on public.workout_sessions (template_id);
create index idx_workout_exercises_session on public.workout_exercises (session_id, order_index);
create index idx_workout_exercises_exercise on public.workout_exercises (exercise_id);
create index idx_workout_sets_we on public.workout_sets (workout_exercise_id, set_number);
create index idx_user_quests_user_assigned on public.user_quests (user_id, assigned_at desc);
create index idx_user_quests_quest on public.user_quests (quest_id);
create index idx_personal_records_user_ex_date on public.personal_records (user_id, exercise_id, achieved_at desc);

-- ============================================================================
-- updated_at triggers (only on tables that carry updated_at)
-- ============================================================================
create trigger trg_profiles_updated_at before update on public.profiles
  for each row execute function public.set_updated_at();
create trigger trg_workout_templates_updated_at before update on public.workout_templates
  for each row execute function public.set_updated_at();
create trigger trg_workout_sessions_updated_at before update on public.workout_sessions
  for each row execute function public.set_updated_at();
create trigger trg_exercise_user_stats_updated_at before update on public.exercise_user_stats
  for each row execute function public.set_updated_at();
create trigger trg_player_progression_updated_at before update on public.player_progression
  for each row execute function public.set_updated_at();

-- ============================================================================
-- Auto-provision profile + progression rows when an auth user is created.
-- SECURITY DEFINER so it can insert regardless of the caller's RLS context.
-- ============================================================================
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email)
  values (new.id, new.email)
  on conflict (id) do nothing;

  insert into public.player_progression (user_id)
  values (new.id)
  on conflict (user_id) do nothing;

  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

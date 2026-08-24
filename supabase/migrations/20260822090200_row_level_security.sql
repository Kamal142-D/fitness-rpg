-- ============================================================================
-- Row-Level Security (PLAN.txt §5 security requirements)
--
-- Rules enforced here:
--   * Every user-owned table has RLS ON; a user can read/write only rows tied
--     to auth.uid().
--   * Reference tables (exercises, quests) are readable by authenticated users
--     but have NO write policies, so writes are denied to anon/authenticated and
--     only possible via the service_role (which bypasses RLS) or an admin.
--   * Ownership is enforced by the database, never trusted from the client.
--
-- Child tables (template exercises, session exercises, sets) are policed through
-- their parent's ownership via EXISTS checks.
-- ============================================================================

-- Enable RLS on every table.
alter table public.profiles                    enable row level security;
alter table public.body_assessments            enable row level security;
alter table public.exercises                   enable row level security;
alter table public.workout_templates           enable row level security;
alter table public.workout_template_exercises  enable row level security;
alter table public.workout_sessions            enable row level security;
alter table public.workout_exercises           enable row level security;
alter table public.workout_sets                enable row level security;
alter table public.exercise_user_stats         enable row level security;
alter table public.player_progression          enable row level security;
alter table public.quests                      enable row level security;
alter table public.user_quests                 enable row level security;
alter table public.personal_records            enable row level security;

-- Baseline privileges. RLS still gates every row; these grants only say which
-- verbs each role may attempt. Reference tables are SELECT-only for users.
grant select, insert, update, delete on
  public.profiles, public.body_assessments, public.workout_templates,
  public.workout_template_exercises, public.workout_sessions,
  public.workout_exercises, public.workout_sets, public.exercise_user_stats,
  public.player_progression, public.user_quests, public.personal_records
  to authenticated;
grant select on public.exercises, public.quests to authenticated;

-- ----------------------------------------------------------------------------
-- profiles
-- ----------------------------------------------------------------------------
create policy "profiles_select_own" on public.profiles
  for select to authenticated using (id = auth.uid());
create policy "profiles_insert_own" on public.profiles
  for insert to authenticated with check (id = auth.uid());
create policy "profiles_update_own" on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- ----------------------------------------------------------------------------
-- body_assessments
-- ----------------------------------------------------------------------------
create policy "body_assessments_select_own" on public.body_assessments
  for select to authenticated using (user_id = auth.uid());
create policy "body_assessments_insert_own" on public.body_assessments
  for insert to authenticated with check (user_id = auth.uid());
create policy "body_assessments_update_own" on public.body_assessments
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "body_assessments_delete_own" on public.body_assessments
  for delete to authenticated using (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- exercises (reference: read-only to authenticated)
-- ----------------------------------------------------------------------------
create policy "exercises_select_all_authenticated" on public.exercises
  for select to authenticated using (true);

-- ----------------------------------------------------------------------------
-- workout_templates (own rows + system templates are readable)
-- ----------------------------------------------------------------------------
create policy "workout_templates_select" on public.workout_templates
  for select to authenticated using (user_id = auth.uid() or is_system_template);
create policy "workout_templates_insert_own" on public.workout_templates
  for insert to authenticated with check (user_id = auth.uid() and not is_system_template);
create policy "workout_templates_update_own" on public.workout_templates
  for update to authenticated
  using (user_id = auth.uid() and not is_system_template)
  with check (user_id = auth.uid() and not is_system_template);
create policy "workout_templates_delete_own" on public.workout_templates
  for delete to authenticated using (user_id = auth.uid() and not is_system_template);

-- ----------------------------------------------------------------------------
-- workout_template_exercises (policed via parent template)
-- ----------------------------------------------------------------------------
create policy "wte_select" on public.workout_template_exercises
  for select to authenticated using (
    exists (
      select 1 from public.workout_templates t
      where t.id = template_id and (t.user_id = auth.uid() or t.is_system_template)
    )
  );
create policy "wte_insert_own" on public.workout_template_exercises
  for insert to authenticated with check (
    exists (
      select 1 from public.workout_templates t
      where t.id = template_id and t.user_id = auth.uid() and not t.is_system_template
    )
  );
create policy "wte_update_own" on public.workout_template_exercises
  for update to authenticated using (
    exists (
      select 1 from public.workout_templates t
      where t.id = template_id and t.user_id = auth.uid() and not t.is_system_template
    )
  ) with check (
    exists (
      select 1 from public.workout_templates t
      where t.id = template_id and t.user_id = auth.uid() and not t.is_system_template
    )
  );
create policy "wte_delete_own" on public.workout_template_exercises
  for delete to authenticated using (
    exists (
      select 1 from public.workout_templates t
      where t.id = template_id and t.user_id = auth.uid() and not t.is_system_template
    )
  );

-- ----------------------------------------------------------------------------
-- workout_sessions
-- ----------------------------------------------------------------------------
create policy "workout_sessions_select_own" on public.workout_sessions
  for select to authenticated using (user_id = auth.uid());
create policy "workout_sessions_insert_own" on public.workout_sessions
  for insert to authenticated with check (user_id = auth.uid());
create policy "workout_sessions_update_own" on public.workout_sessions
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "workout_sessions_delete_own" on public.workout_sessions
  for delete to authenticated using (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- workout_exercises (policed via parent session)
-- ----------------------------------------------------------------------------
create policy "workout_exercises_all_own" on public.workout_exercises
  for all to authenticated using (
    exists (
      select 1 from public.workout_sessions s
      where s.id = session_id and s.user_id = auth.uid()
    )
  ) with check (
    exists (
      select 1 from public.workout_sessions s
      where s.id = session_id and s.user_id = auth.uid()
    )
  );

-- ----------------------------------------------------------------------------
-- workout_sets (policed via parent exercise -> session)
-- ----------------------------------------------------------------------------
create policy "workout_sets_all_own" on public.workout_sets
  for all to authenticated using (
    exists (
      select 1
      from public.workout_exercises we
      join public.workout_sessions s on s.id = we.session_id
      where we.id = workout_exercise_id and s.user_id = auth.uid()
    )
  ) with check (
    exists (
      select 1
      from public.workout_exercises we
      join public.workout_sessions s on s.id = we.session_id
      where we.id = workout_exercise_id and s.user_id = auth.uid()
    )
  );

-- ----------------------------------------------------------------------------
-- exercise_user_stats
-- ----------------------------------------------------------------------------
create policy "exercise_user_stats_all_own" on public.exercise_user_stats
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- player_progression
-- ----------------------------------------------------------------------------
create policy "player_progression_select_own" on public.player_progression
  for select to authenticated using (user_id = auth.uid());
create policy "player_progression_insert_own" on public.player_progression
  for insert to authenticated with check (user_id = auth.uid());
create policy "player_progression_update_own" on public.player_progression
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- quests (reference: active ones, plus any the user has been assigned)
-- ----------------------------------------------------------------------------
create policy "quests_select_active_or_assigned" on public.quests
  for select to authenticated using (
    active
    or exists (
      select 1 from public.user_quests uq
      where uq.quest_id = quests.id and uq.user_id = auth.uid()
    )
  );

-- ----------------------------------------------------------------------------
-- user_quests
-- ----------------------------------------------------------------------------
create policy "user_quests_all_own" on public.user_quests
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- ----------------------------------------------------------------------------
-- personal_records
-- ----------------------------------------------------------------------------
create policy "personal_records_all_own" on public.personal_records
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

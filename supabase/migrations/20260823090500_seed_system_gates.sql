-- ============================================================================
-- Seed: system Gates (workout_templates) + their exercises (Phase 6).
--
-- Six starter Gates: Push, Pull, Legs, Upper, Lower, Full Body. Each is a
-- system template (user_id NULL, is_system_template true) readable by all
-- authenticated users. `difficulty` here is the Gate Difficulty (chosen before
-- training) — NOT a Gate Clear Rank (computed after a workout).
--
-- Idempotent: fixed template UUIDs with ON CONFLICT DO NOTHING, and template
-- exercises keyed by (template_id, order_index). Exercise ids are resolved by
-- name from the exercises seed (20260822090400_seed_exercises.sql).
-- ============================================================================

insert into public.workout_templates
  (id, user_id, name, description, estimated_duration_minutes, difficulty, is_system_template)
values
  ('10000000-0000-0000-0000-000000000001', null, 'Push',                'Chest, shoulders, triceps',        50, 'C', true),
  ('10000000-0000-0000-0000-000000000002', null, 'Pull',                'Back, biceps, rear delts',         50, 'C', true),
  ('10000000-0000-0000-0000-000000000003', null, 'Legs',                'Quads, hamstrings, glutes, calves', 55, 'C', true),
  ('10000000-0000-0000-0000-000000000004', null, 'Upper',               'Chest, back, shoulders, arms',     60, 'B', true),
  ('10000000-0000-0000-0000-000000000005', null, 'Lower',               'Quads, hamstrings, glutes, calves', 55, 'B', true),
  ('10000000-0000-0000-0000-000000000006', null, 'Full Body — Initiation', 'Full body',                     45, 'D', true)
on conflict (id) do nothing;

-- Helper note: each row resolves exercise_id from the catalog by unique name.

insert into public.workout_template_exercises
  (template_id, exercise_id, order_index, target_sets, target_reps_min, target_reps_max, target_rpe, rest_seconds)
values
  -- Push
  ('10000000-0000-0000-0000-000000000001', (select id from public.exercises where name = 'Barbell Bench Press'),    0, 4, 5, 8, 8, 150),
  ('10000000-0000-0000-0000-000000000001', (select id from public.exercises where name = 'Overhead Press'),         1, 4, 6, 10, 8, 150),
  ('10000000-0000-0000-0000-000000000001', (select id from public.exercises where name = 'Incline Dumbbell Press'), 2, 3, 8, 12, 8, 120),
  ('10000000-0000-0000-0000-000000000001', (select id from public.exercises where name = 'Lateral Raise'),          3, 3, 12, 20, 8, 60),
  ('10000000-0000-0000-0000-000000000001', (select id from public.exercises where name = 'Triceps Pushdown'),       4, 3, 10, 15, 8, 75),
  -- Pull
  ('10000000-0000-0000-0000-000000000002', (select id from public.exercises where name = 'Barbell Bent-Over Row'),  0, 4, 6, 10, 8, 150),
  ('10000000-0000-0000-0000-000000000002', (select id from public.exercises where name = 'Pull-Up'),                1, 4, 5, 10, 8, 150),
  ('10000000-0000-0000-0000-000000000002', (select id from public.exercises where name = 'Lat Pulldown'),           2, 3, 8, 12, 8, 90),
  ('10000000-0000-0000-0000-000000000002', (select id from public.exercises where name = 'Seated Cable Row'),       3, 3, 10, 12, 8, 90),
  ('10000000-0000-0000-0000-000000000002', (select id from public.exercises where name = 'Face Pull'),              4, 3, 15, 20, 8, 60),
  ('10000000-0000-0000-0000-000000000002', (select id from public.exercises where name = 'Barbell Curl'),           5, 3, 8, 12, 8, 75),
  -- Legs
  ('10000000-0000-0000-0000-000000000003', (select id from public.exercises where name = 'Barbell Back Squat'),     0, 4, 5, 8, 8, 180),
  ('10000000-0000-0000-0000-000000000003', (select id from public.exercises where name = 'Romanian Deadlift'),      1, 3, 8, 12, 8, 150),
  ('10000000-0000-0000-0000-000000000003', (select id from public.exercises where name = 'Leg Press'),              2, 3, 10, 15, 8, 120),
  ('10000000-0000-0000-0000-000000000003', (select id from public.exercises where name = 'Lying Leg Curl'),         3, 3, 10, 15, 8, 75),
  ('10000000-0000-0000-0000-000000000003', (select id from public.exercises where name = 'Standing Calf Raise'),    4, 4, 10, 15, 8, 60),
  -- Upper
  ('10000000-0000-0000-0000-000000000004', (select id from public.exercises where name = 'Barbell Bench Press'),    0, 4, 5, 8, 8, 150),
  ('10000000-0000-0000-0000-000000000004', (select id from public.exercises where name = 'Barbell Bent-Over Row'),  1, 4, 6, 10, 8, 150),
  ('10000000-0000-0000-0000-000000000004', (select id from public.exercises where name = 'Overhead Press'),         2, 3, 8, 12, 8, 120),
  ('10000000-0000-0000-0000-000000000004', (select id from public.exercises where name = 'Lat Pulldown'),           3, 3, 8, 12, 8, 90),
  ('10000000-0000-0000-0000-000000000004', (select id from public.exercises where name = 'Dumbbell Curl'),          4, 3, 10, 12, 8, 60),
  ('10000000-0000-0000-0000-000000000004', (select id from public.exercises where name = 'Triceps Pushdown'),       5, 3, 10, 15, 8, 60),
  -- Lower
  ('10000000-0000-0000-0000-000000000005', (select id from public.exercises where name = 'Barbell Back Squat'),     0, 4, 5, 8, 8, 180),
  ('10000000-0000-0000-0000-000000000005', (select id from public.exercises where name = 'Romanian Deadlift'),      1, 3, 8, 12, 8, 150),
  ('10000000-0000-0000-0000-000000000005', (select id from public.exercises where name = 'Leg Press'),              2, 3, 10, 15, 8, 120),
  ('10000000-0000-0000-0000-000000000005', (select id from public.exercises where name = 'Leg Extension'),          3, 3, 12, 15, 8, 60),
  ('10000000-0000-0000-0000-000000000005', (select id from public.exercises where name = 'Standing Calf Raise'),    4, 4, 10, 15, 8, 60),
  -- Full Body — Initiation
  ('10000000-0000-0000-0000-000000000006', (select id from public.exercises where name = 'Barbell Back Squat'),     0, 3, 5, 8, 7, 150),
  ('10000000-0000-0000-0000-000000000006', (select id from public.exercises where name = 'Barbell Bench Press'),    1, 3, 5, 8, 7, 150),
  ('10000000-0000-0000-0000-000000000006', (select id from public.exercises where name = 'Single-Arm Dumbbell Row'), 2, 3, 8, 12, 7, 90),
  ('10000000-0000-0000-0000-000000000006', (select id from public.exercises where name = 'Overhead Press'),         3, 3, 8, 12, 7, 90),
  ('10000000-0000-0000-0000-000000000006', (select id from public.exercises where name = 'Plank'),                  4, 3, null, null, null, 60)
on conflict (template_id, order_index) do nothing;

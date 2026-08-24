-- ============================================================================
-- Seed: common gym exercises (PLAN.txt Phase 2). Reference data, so it lives in
-- a migration (applied on every environment via db push / db reset), not just
-- the local-only seed.sql. Idempotent via ON CONFLICT on the unique name.
--
-- ranking_enabled = false for isometric / non-load-progressive movements the
-- strength engine should not try to rank with a 1RM formula.
-- ============================================================================

insert into public.exercises
  (name, category, primary_muscle_group, secondary_muscle_groups, equipment, exercise_type, ranking_enabled)
values
  -- Chest
  ('Barbell Bench Press',        'chest',     'chest',       array['triceps','front delts'], 'barbell',    'strength',   true),
  ('Incline Dumbbell Press',     'chest',     'upper chest', array['front delts','triceps'], 'dumbbell',   'strength',   true),
  ('Dumbbell Bench Press',       'chest',     'chest',       array['triceps','front delts'], 'dumbbell',   'strength',   true),
  ('Push-Up',                    'chest',     'chest',       array['triceps','front delts','core'], 'bodyweight', 'bodyweight', true),
  ('Cable Chest Fly',            'chest',     'chest',       array['front delts'],           'cable',      'strength',   true),

  -- Back
  ('Deadlift',                   'back',      'back',        array['hamstrings','glutes','forearms','core'], 'barbell', 'strength', true),
  ('Barbell Bent-Over Row',      'back',      'lats',        array['biceps','rear delts','forearms'], 'barbell', 'strength', true),
  ('Pull-Up',                    'back',      'lats',        array['biceps','forearms'],     'bodyweight', 'bodyweight', true),
  ('Lat Pulldown',               'back',      'lats',        array['biceps'],                'cable',      'strength',   true),
  ('Seated Cable Row',           'back',      'mid back',    array['biceps','rear delts'],   'cable',      'strength',   true),
  ('Single-Arm Dumbbell Row',    'back',      'lats',        array['biceps','forearms'],     'dumbbell',   'strength',   true),

  -- Legs
  ('Barbell Back Squat',         'legs',      'quadriceps',  array['glutes','core'],         'barbell',    'strength',   true),
  ('Front Squat',                'legs',      'quadriceps',  array['glutes','core'],         'barbell',    'strength',   true),
  ('Romanian Deadlift',          'legs',      'hamstrings',  array['glutes','back'],         'barbell',    'strength',   true),
  ('Leg Press',                  'legs',      'quadriceps',  array['glutes'],                'machine',    'strength',   true),
  ('Walking Lunge',              'legs',      'quadriceps',  array['glutes','core'],         'dumbbell',   'strength',   true),
  ('Lying Leg Curl',             'legs',      'hamstrings',  array[]::text[],                'machine',    'strength',   true),
  ('Leg Extension',              'legs',      'quadriceps',  array[]::text[],                'machine',    'strength',   true),
  ('Standing Calf Raise',        'legs',      'calves',      array[]::text[],                'machine',    'strength',   true),

  -- Shoulders
  ('Overhead Press',             'shoulders', 'front delts', array['triceps','core'],        'barbell',    'strength',   true),
  ('Dumbbell Shoulder Press',    'shoulders', 'front delts', array['triceps'],               'dumbbell',   'strength',   true),
  ('Lateral Raise',              'shoulders', 'side delts',  array[]::text[],                'dumbbell',   'strength',   true),
  ('Face Pull',                  'shoulders', 'rear delts',  array['upper back'],            'cable',      'strength',   true),

  -- Arms
  ('Barbell Curl',               'arms',      'biceps',      array['forearms'],              'barbell',    'strength',   true),
  ('Dumbbell Curl',              'arms',      'biceps',      array['forearms'],              'dumbbell',   'strength',   true),
  ('Hammer Curl',                'arms',      'biceps',      array['forearms'],              'dumbbell',   'strength',   true),
  ('Triceps Pushdown',           'arms',      'triceps',     array[]::text[],                'cable',      'strength',   true),
  ('Overhead Triceps Extension', 'arms',      'triceps',     array[]::text[],                'dumbbell',   'strength',   true),

  -- Core
  ('Plank',                      'core',      'core',        array['front delts'],           'bodyweight', 'bodyweight', false),
  ('Hanging Leg Raise',          'core',      'core',        array['hip flexors'],           'bodyweight', 'bodyweight', true),
  ('Cable Crunch',               'core',      'core',        array[]::text[],                'cable',      'strength',   true),
  ('Russian Twist',              'core',      'obliques',    array['core'],                  'bodyweight', 'bodyweight', false)
on conflict (name) do nothing;

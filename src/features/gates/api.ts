import type {
  CreateGateInput,
  Exercise,
  GateDetail,
  GateTemplate,
  TemplateExercise,
} from '@/features/gates/types';
import { supabase } from '@/services/supabase';
import type { TablesInsert } from '@/types/database';

const FULL_BODY_TEMPLATE_ID = '10000000-0000-0000-0000-000000000006';

/** Default per-exercise targets applied to a custom Gate's exercises. */
const DEFAULT_TARGETS = {
  target_sets: 3,
  target_reps_min: 8,
  target_reps_max: 12,
  target_rpe: 8,
  rest_seconds: 90,
} as const;

/** All Gates visible to the user (system templates + their own). RLS filters. */
export async function listGates(): Promise<GateTemplate[]> {
  const { data, error } = await supabase
    .from('workout_templates')
    .select('*')
    .order('is_system_template', { ascending: false })
    .order('name', { ascending: true });
  if (error) throw error;
  return data ?? [];
}

/** A Gate plus its ordered exercises (joined in JS to stay fully typed). */
export async function getGate(templateId: string): Promise<GateDetail | null> {
  const { data: template, error: tErr } = await supabase
    .from('workout_templates')
    .select('*')
    .eq('id', templateId)
    .maybeSingle();
  if (tErr) throw tErr;
  if (!template) return null;

  const { data: tes, error: teErr } = await supabase
    .from('workout_template_exercises')
    .select('*')
    .eq('template_id', templateId)
    .order('order_index', { ascending: true });
  if (teErr) throw teErr;
  const templateExercises: TemplateExercise[] = tes ?? [];

  const exerciseIds = templateExercises.map((te) => te.exercise_id);
  let exercises: Exercise[] = [];
  if (exerciseIds.length > 0) {
    const { data: exs, error: exErr } = await supabase
      .from('exercises')
      .select('*')
      .in('id', exerciseIds);
    if (exErr) throw exErr;
    exercises = exs ?? [];
  }
  const byId = new Map(exercises.map((e) => [e.id, e]));

  return {
    template,
    exercises: templateExercises
      .filter((te) => byId.has(te.exercise_id))
      .map((te) => ({ ...te, exercise: byId.get(te.exercise_id)! })),
  };
}

/** The recommended "Today's Gate": the Full Body starter, else the first system Gate. */
export async function getRecommendedGate(): Promise<GateTemplate | null> {
  const { data: fullBody } = await supabase
    .from('workout_templates')
    .select('*')
    .eq('id', FULL_BODY_TEMPLATE_ID)
    .maybeSingle();
  if (fullBody) return fullBody;

  const { data, error } = await supabase
    .from('workout_templates')
    .select('*')
    .eq('is_system_template', true)
    .order('name')
    .limit(1);
  if (error) throw error;
  return data?.[0] ?? null;
}

/** The exercise catalog (for building custom Gates). */
export async function listExercises(): Promise<Exercise[]> {
  const { data, error } = await supabase
    .from('exercises')
    .select('*')
    .order('category', { ascending: true })
    .order('name', { ascending: true });
  if (error) throw error;
  return data ?? [];
}

/** Create a custom Gate owned by the user, with default targets per exercise. */
export async function createGate(userId: string, input: CreateGateInput): Promise<string> {
  const templateRow: TablesInsert<'workout_templates'> = {
    user_id: userId,
    name: input.name.trim(),
    difficulty: input.difficulty,
    is_system_template: false,
    estimated_duration_minutes: Math.min(360, Math.max(10, input.exerciseIds.length * 9)),
    description: 'Custom gate',
  };
  const { data: template, error: tErr } = await supabase
    .from('workout_templates')
    .insert(templateRow)
    .select('id')
    .single();
  if (tErr) throw tErr;

  const rows: TablesInsert<'workout_template_exercises'>[] = input.exerciseIds.map(
    (exerciseId, i) => ({
      template_id: template.id,
      exercise_id: exerciseId,
      order_index: i,
      ...DEFAULT_TARGETS,
    }),
  );
  const { error: teErr } = await supabase.from('workout_template_exercises').insert(rows);
  if (teErr) throw teErr;

  return template.id;
}

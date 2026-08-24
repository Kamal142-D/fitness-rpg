import type { Rank } from '@/constants/ranks';
import type { Tables } from '@/types/database';

export type GateTemplate = Tables<'workout_templates'>;
export type TemplateExercise = Tables<'workout_template_exercises'>;
export type Exercise = Tables<'exercises'>;

/** A template joined with its ordered exercises + exercise catalog info. */
export interface GateDetail {
  template: GateTemplate;
  exercises: (TemplateExercise & { exercise: Exercise })[];
}

/** Input for creating a custom Gate. */
export interface CreateGateInput {
  name: string;
  difficulty: Rank;
  exerciseIds: string[];
}

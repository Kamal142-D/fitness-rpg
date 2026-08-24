export type {
  GateTemplate,
  TemplateExercise,
  Exercise,
  GateDetail,
  CreateGateInput,
} from '@/features/gates/types';
export {
  intensityForDifficulty,
  templateDifficulty,
  muscleGroupsFor,
  templateToSuggestedGate,
  formatRepRange,
  formatTargets,
} from '@/features/gates/mappers';
export { validateCreateGate, createGateHasErrors } from '@/features/gates/schemas';
export type { CreateGateErrors, CreateGateDraft } from '@/features/gates/schemas';
export {
  listGates,
  getGate,
  getRecommendedGate,
  listExercises,
  createGate,
} from '@/features/gates/api';
export {
  useGates,
  useGate,
  useRecommendedGate,
  useExercises,
  useCreateGate,
} from '@/features/gates/hooks';

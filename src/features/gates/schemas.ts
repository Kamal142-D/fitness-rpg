import type { Rank } from '@/constants/ranks';

export type CreateGateErrors = Partial<Record<'name' | 'difficulty' | 'exercises', string>>;

/** Draft shape for validation (difficulty may be unset in the form). */
export interface CreateGateDraft {
  name: string;
  difficulty: Rank | null;
  exerciseIds: string[];
}

/** Validate custom-Gate creation input. */
export function validateCreateGate(input: CreateGateDraft): CreateGateErrors {
  const e: CreateGateErrors = {};
  const name = input.name.trim();
  if (!name) e.name = 'Name your Gate';
  else if (name.length > 60) e.name = 'Keep the name under 60 characters';

  if (!input.difficulty) e.difficulty = 'Choose a difficulty';

  if (input.exerciseIds.length < 1) e.exercises = 'Add at least one exercise';
  else if (input.exerciseIds.length > 15) e.exercises = 'Keep it to 15 exercises or fewer';

  return e;
}

export function createGateHasErrors(errors: CreateGateErrors): boolean {
  return Object.keys(errors).length > 0;
}

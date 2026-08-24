import { createGateHasErrors, validateCreateGate } from '@/features/gates/schemas';

describe('validateCreateGate', () => {
  it('flags an empty name, missing difficulty, and no exercises', () => {
    const e = validateCreateGate({ name: '  ', difficulty: null, exerciseIds: [] });
    expect(e.name).toBeDefined();
    expect(e.difficulty).toBeDefined();
    expect(e.exercises).toBeDefined();
    expect(createGateHasErrors(e)).toBe(true);
  });

  it('passes a valid draft', () => {
    const e = validateCreateGate({
      name: 'Chest & Arms',
      difficulty: 'C',
      exerciseIds: ['a', 'b'],
    });
    expect(createGateHasErrors(e)).toBe(false);
  });

  it('rejects an overly long name and too many exercises', () => {
    const longName = 'x'.repeat(61);
    const many = Array.from({ length: 16 }, (_, i) => String(i));
    const e = validateCreateGate({ name: longName, difficulty: 'B', exerciseIds: many });
    expect(e.name).toBeDefined();
    expect(e.exercises).toBeDefined();
  });
});

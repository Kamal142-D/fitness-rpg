import {
  meetsQualifyingThreshold,
  qualifyingWorkingSets,
  validateWorkingSet,
} from '@/services/ranking/validation';
import type { RankingSetInput } from '@/services/ranking/validation';

function s(o: Partial<RankingSetInput>): RankingSetInput {
  return { weightKg: 100, reps: 5, rpe: 8, isWarmup: false, isCompleted: true, ...o };
}

describe('validateWorkingSet', () => {
  it('accepts a plausible completed working set', () => {
    expect(validateWorkingSet(s({})).valid).toBe(true);
  });
  it('rejects incomplete and warm-up sets', () => {
    expect(validateWorkingSet(s({ isCompleted: false })).valid).toBe(false);
    expect(validateWorkingSet(s({ isWarmup: true })).valid).toBe(false);
  });
  it('rejects missing load or reps', () => {
    expect(validateWorkingSet(s({ weightKg: null })).valid).toBe(false);
    expect(validateWorkingSet(s({ weightKg: 0 })).valid).toBe(false);
    expect(validateWorkingSet(s({ reps: null })).valid).toBe(false);
  });
  it('rejects implausible weight, reps, and rpe', () => {
    expect(validateWorkingSet(s({ weightKg: 9999 })).valid).toBe(false);
    expect(validateWorkingSet(s({ reps: 500 })).valid).toBe(false);
    expect(validateWorkingSet(s({ rpe: 11 })).valid).toBe(false);
  });
  it('excludes reps above the strength cap of 12', () => {
    expect(validateWorkingSet(s({ reps: 13 })).valid).toBe(false);
  });
  it('gives a reason for every rejection', () => {
    expect(validateWorkingSet(s({ isWarmup: true })).reason).toBeTruthy();
  });
});

describe('qualifyingWorkingSets & threshold', () => {
  it('keeps only valid sets', () => {
    const sets = [s({}), s({ isWarmup: true }), s({ weightKg: null }), s({ reps: 3 })];
    expect(qualifyingWorkingSets(sets)).toHaveLength(2);
  });
  it('needs at least two qualifying sets', () => {
    expect(meetsQualifyingThreshold(1)).toBe(false);
    expect(meetsQualifyingThreshold(2)).toBe(true);
  });
});

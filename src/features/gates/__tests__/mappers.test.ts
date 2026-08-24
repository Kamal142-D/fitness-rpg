import {
  formatRepRange,
  formatTargets,
  intensityForDifficulty,
  muscleGroupsFor,
  templateDifficulty,
  templateToSuggestedGate,
} from '@/features/gates/mappers';
import type { GateTemplate } from '@/features/gates/types';

function template(overrides: Partial<GateTemplate> = {}): GateTemplate {
  return {
    id: 't1',
    user_id: null,
    name: 'Push',
    description: 'Chest, shoulders, triceps',
    estimated_duration_minutes: 50,
    difficulty: 'C',
    is_system_template: true,
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('intensityForDifficulty', () => {
  it('maps every rank to a label', () => {
    expect(intensityForDifficulty('E')).toBe('Light');
    expect(intensityForDifficulty('D')).toBe('Light');
    expect(intensityForDifficulty('C')).toBe('Moderate');
    expect(intensityForDifficulty('B')).toBe('Hard');
    expect(intensityForDifficulty('A')).toBe('Brutal');
    expect(intensityForDifficulty('S')).toBe('Brutal');
  });
});

describe('templateDifficulty', () => {
  it('passes valid ranks through', () => {
    expect(templateDifficulty({ difficulty: 'B' })).toBe('B');
  });
  it('defaults null/invalid to D', () => {
    expect(templateDifficulty({ difficulty: null })).toBe('D');
    expect(templateDifficulty({ difficulty: 'Z' })).toBe('D');
  });
});

describe('muscleGroupsFor', () => {
  it('splits a description into groups', () => {
    expect(muscleGroupsFor({ description: 'Chest, shoulders, triceps' })).toEqual([
      'Chest',
      'shoulders',
      'triceps',
    ]);
  });
  it('returns [] for a null description', () => {
    expect(muscleGroupsFor({ description: null })).toEqual([]);
  });
});

describe('templateToSuggestedGate', () => {
  it('maps a template into the dashboard shape', () => {
    const g = templateToSuggestedGate(template());
    expect(g.name).toBe('Push');
    expect(g.difficulty).toBe('C');
    expect(g.durationMinutes).toBe(50);
    expect(g.intensity).toBe('Moderate');
    expect(g.muscleGroups).toContain('Chest');
  });
  it('falls back to a default duration when unset', () => {
    expect(
      templateToSuggestedGate(template({ estimated_duration_minutes: null })).durationMinutes,
    ).toBe(45);
  });
});

describe('formatRepRange / formatTargets', () => {
  it('formats rep ranges', () => {
    expect(formatRepRange(5, 8)).toBe('5-8');
    expect(formatRepRange(8, 8)).toBe('8');
    expect(formatRepRange(5, null)).toBe('5+');
    expect(formatRepRange(null, null)).toBe('—');
  });
  it('formats sets × reps, and time-based (no reps)', () => {
    expect(formatTargets({ target_sets: 4, target_reps_min: 5, target_reps_max: 8 })).toBe(
      '4 × 5-8',
    );
    expect(formatTargets({ target_sets: 3, target_reps_min: null, target_reps_max: null })).toBe(
      '3 sets',
    );
  });
});

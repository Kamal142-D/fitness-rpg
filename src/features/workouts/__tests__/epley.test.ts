import { estimatedOneRepMax } from '@/features/workouts/epley';

describe('estimatedOneRepMax (Epley)', () => {
  it('returns the weight itself at 1 rep', () => {
    expect(estimatedOneRepMax(100, 1)).toBe(100);
  });
  it('applies weight * (1 + reps/30)', () => {
    expect(estimatedOneRepMax(100, 5)).toBe(116.67);
    expect(estimatedOneRepMax(100, 10)).toBe(133.33);
  });
  it('rejects reps outside 1..12 and non-positive weight', () => {
    expect(estimatedOneRepMax(100, 0)).toBeNull();
    expect(estimatedOneRepMax(100, 13)).toBeNull();
    expect(estimatedOneRepMax(0, 5)).toBeNull();
    expect(estimatedOneRepMax(-50, 5)).toBeNull();
  });
  it('returns null when weight or reps is null', () => {
    expect(estimatedOneRepMax(null, 5)).toBeNull();
    expect(estimatedOneRepMax(100, null)).toBeNull();
  });
});

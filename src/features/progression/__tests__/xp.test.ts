import { getXpRequiredForLevel, xpProgress } from '@/features/progression/xp';

describe('getXpRequiredForLevel', () => {
  it('matches the documented curve 100 * level^1.5 (PLAN.txt §6.10)', () => {
    expect(getXpRequiredForLevel(1)).toBe(100);
    expect(getXpRequiredForLevel(4)).toBe(800); // 100 * 8
    expect(getXpRequiredForLevel(9)).toBe(2700); // 100 * 27
    expect(getXpRequiredForLevel(16)).toBe(6400); // 100 * 64
  });

  it('is strictly increasing across many levels', () => {
    for (let l = 1; l < 60; l++) {
      expect(getXpRequiredForLevel(l + 1)).toBeGreaterThan(getXpRequiredForLevel(l));
    }
  });

  it('floors fractional levels and treats sub-1 as level 1', () => {
    expect(getXpRequiredForLevel(4.9)).toBe(getXpRequiredForLevel(4));
    expect(getXpRequiredForLevel(0)).toBe(100);
    expect(getXpRequiredForLevel(-5)).toBe(100);
  });
});

describe('xpProgress', () => {
  it('reports current/required/fraction within a level', () => {
    const p = xpProgress(50, 1);
    expect(p.required).toBe(100);
    expect(p.current).toBe(50);
    expect(p.fraction).toBeCloseTo(0.5);
  });

  it('clamps current XP to [0, required] and fraction to [0, 1]', () => {
    const over = xpProgress(9999, 1);
    expect(over.current).toBe(100);
    expect(over.fraction).toBe(1);
    const under = xpProgress(-20, 1);
    expect(under.current).toBe(0);
    expect(under.fraction).toBe(0);
  });
});

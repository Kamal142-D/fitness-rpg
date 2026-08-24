import { adherenceRatio, disciplineScore, updateStreak } from '@/services/ranking/streak';

describe('updateStreak (rest-safe, PLAN.txt §6.9)', () => {
  it('increments on training and tracks the longest', () => {
    let s = { current: 0, longest: 0 };
    s = updateStreak(s, { didTrain: true, isScheduledRest: false });
    s = updateStreak(s, { didTrain: true, isScheduledRest: false });
    expect(s).toEqual({ current: 2, longest: 2 });
  });
  it('preserves the streak on a scheduled rest day', () => {
    const s = updateStreak({ current: 3, longest: 5 }, { didTrain: false, isScheduledRest: true });
    expect(s).toEqual({ current: 3, longest: 5 });
  });
  it('resets on a missed (non-rest) training day but keeps the longest', () => {
    const s = updateStreak({ current: 4, longest: 6 }, { didTrain: false, isScheduledRest: false });
    expect(s).toEqual({ current: 0, longest: 6 });
  });
});

describe('adherenceRatio', () => {
  it('is completed/planned, clamped to 0..1', () => {
    expect(adherenceRatio(3, 4)).toBe(0.75);
    expect(adherenceRatio(5, 4)).toBe(1);
    expect(adherenceRatio(0, 0)).toBe(0);
  });
});

describe('disciplineScore (capped streak bonus, no unsafe incentive)', () => {
  it('weights adherence with a capped streak bonus', () => {
    expect(disciplineScore({ currentStreakDays: 0, adherence: 1 })).toBe(75);
    expect(disciplineScore({ currentStreakDays: 10, adherence: 1 })).toBe(100);
    expect(disciplineScore({ currentStreakDays: 4, adherence: 0.5 })).toBeCloseTo(47.5, 5);
  });
  it('caps the streak contribution so a huge streak cannot dominate', () => {
    expect(disciplineScore({ currentStreakDays: 999, adherence: 0 })).toBe(25);
  });
});

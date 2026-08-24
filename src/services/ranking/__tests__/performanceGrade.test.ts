import { performanceGrade, performanceScore } from '@/services/ranking/performanceGrade';

describe('performanceScore (today vs baseline)', () => {
  it('is neutral with no baseline or no valid effort (new users not punished)', () => {
    expect(performanceScore(120, null)).toBe(60);
    expect(performanceScore(null, 100)).toBe(60);
  });
  it('scores at/above and below the baseline', () => {
    expect(performanceScore(100, 100)).toBeCloseTo(65, 5); // ratio 1.0
    expect(performanceScore(110, 100)).toBeCloseTo(88, 5); // ratio 1.1
    expect(performanceScore(90, 100)).toBeCloseTo(40, 5); // ratio 0.9
  });
  it('maps to a grade letter', () => {
    expect(performanceGrade(performanceScore(100, 100))).toBe('A'); // 65 -> A
    expect(performanceGrade(performanceScore(90, 100))).toBe('C'); // 40 -> C
  });
});

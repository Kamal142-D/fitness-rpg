import {
  completionScore,
  computeGateScore,
  gateClearRank,
  prComponentScore,
  progressScore,
  qualityScore,
  weightedRenormalized,
} from '@/services/ranking/gateScore';

describe('computeGateScore (PLAN.txt §6.5 weighting)', () => {
  it('applies the documented weights', () => {
    // 80*.5 + 100*.2 + 60*.15 + 50*.1 + 60*.05 = 77
    const score = computeGateScore({
      performance: 80,
      completion: 100,
      progress: 60,
      pr: 50,
      quality: 60,
    });
    expect(score).toBeCloseTo(77, 5);
    expect(gateClearRank(score)).toBe('A');
  });

  it('renormalizes when a factor is missing (no history) instead of scoring zero', () => {
    // Drop progress (weight .15). Remaining weights sum .85.
    // (80*.5 + 100*.2 + 50*.1 + 60*.05) / .85 = 68 / .85 = 80
    const score = computeGateScore({
      performance: 80,
      completion: 100,
      progress: null,
      pr: 50,
      quality: 60,
    });
    expect(score).toBeCloseTo(80, 5);
  });

  it('clamps out-of-range component values', () => {
    expect(
      computeGateScore({ performance: 500, completion: 500, progress: 500, pr: 500, quality: 500 }),
    ).toBe(100);
  });

  it('returns the neutral score when every factor is missing', () => {
    expect(weightedRenormalized([{ value: null, weight: 1 }])).toBe(60);
  });
});

describe('gate components', () => {
  it('completionScore is completed/planned percent, clamped', () => {
    expect(completionScore(3, 5)).toBe(60);
    expect(completionScore(10, 5)).toBe(100);
    expect(completionScore(2, 0)).toBe(100);
    expect(completionScore(0, 0)).toBe(0);
  });
  it('progressScore is null without history and a ratio otherwise', () => {
    expect(progressScore(1000, null)).toBeNull();
    expect(progressScore(1100, 1000)).toBeCloseTo(88, 5); // ratio 1.1
  });
  it('prComponentScore rewards PRs without punishing their absence', () => {
    expect(prComponentScore(0)).toBe(50);
    expect(prComponentScore(1)).toBe(72);
    expect(prComponentScore(2)).toBe(86);
    expect(prComponentScore(5)).toBe(100);
  });
  it('qualityScore reflects valid-RPE fraction, neutral when none logged', () => {
    expect(qualityScore([])).toBe(60);
    expect(qualityScore([null, null])).toBe(60);
    expect(qualityScore([8, 9])).toBe(100);
    expect(qualityScore([8, 11])).toBe(50);
  });
});

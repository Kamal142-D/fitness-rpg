import {
  enduranceScore,
  hunterRank,
  hunterScore,
  limitingAttribute,
  physiqueScore,
  strengthScore,
} from '@/services/ranking/attributes';

describe('strengthScore', () => {
  it('averages ranked exercise scores, or null when none', () => {
    expect(strengthScore([])).toBeNull();
    expect(strengthScore([40, 60, 80])).toBe(60);
  });
});

describe('physiqueScore (provisional, non-punitive)', () => {
  it('is null without an assessment', () => {
    expect(physiqueScore(null)).toBeNull();
  });
  it('peaks a healthy body fat and blends with muscle development', () => {
    // bodyFat 12% (male) -> 100; SMM 36/80 = 45% -> ~91.7; mean -> ~95.8
    const score = physiqueScore({
      bodyFatPercent: 12,
      skeletalMuscleMassKg: 36,
      weightKg: 80,
      sex: 'male',
    });
    expect(score).toBeCloseTo(95.8, 1);
  });
  it('does not reward ever-lower body fat (very low scores below the healthy peak)', () => {
    const healthy = physiqueScore({
      bodyFatPercent: 12,
      skeletalMuscleMassKg: null,
      weightKg: null,
      sex: 'male',
    })!;
    const veryLow = physiqueScore({
      bodyFatPercent: 4,
      skeletalMuscleMassKg: null,
      weightKg: null,
      sex: 'male',
    })!;
    expect(veryLow).toBeLessThan(healthy);
  });
});

describe('enduranceScore', () => {
  it('maps weekly training minutes, null when no data', () => {
    expect(enduranceScore(null)).toBeNull();
    expect(enduranceScore(0)).toBe(0);
    expect(enduranceScore(150)).toBe(70);
  });
});

describe('hunterScore & rank (PLAN.txt §6.7)', () => {
  it('applies attribute weights', () => {
    // 80*.4 + 60*.3 + 40*.15 + 60*.15 = 65
    const score = hunterScore({ strength: 80, physique: 60, endurance: 40, discipline: 60 });
    expect(score).toBeCloseTo(65, 5);
    expect(hunterRank(score)).toBe('A');
  });
  it('renormalizes over available attributes (missing physique/endurance/discipline)', () => {
    expect(hunterScore({ strength: 80, physique: null, endurance: null, discipline: null })).toBe(
      80,
    );
  });
  it('is neutral when nothing is known', () => {
    expect(hunterScore({ strength: null, physique: null, endurance: null, discipline: null })).toBe(
      60,
    );
  });
});

describe('limitingAttribute', () => {
  it('returns the lowest available attribute', () => {
    expect(limitingAttribute({ strength: 80, physique: 60, endurance: 40, discipline: 60 })).toBe(
      'endurance',
    );
    expect(
      limitingAttribute({ strength: 80, physique: null, endurance: null, discipline: 70 }),
    ).toBe('discipline');
    expect(
      limitingAttribute({ strength: null, physique: null, endurance: null, discipline: null }),
    ).toBeNull();
  });
});

import { kgToLb, lbToKg, roundTo } from '@/utils/units';

describe('unit conversion', () => {
  it('converts kg to lb and back', () => {
    expect(kgToLb(100)).toBeCloseTo(220.462, 3);
    expect(lbToKg(220.462)).toBeCloseTo(100, 3);
  });
  it('round-trips without drift', () => {
    expect(lbToKg(kgToLb(60))).toBeCloseTo(60, 6);
  });
  it('rounds to a given precision', () => {
    expect(roundTo(220.4622, 1)).toBe(220.5);
    expect(roundTo(1.2345, 2)).toBe(1.23);
  });
});

import { interpolate } from '@/services/ranking/interp';

describe('interpolate', () => {
  const anchors = [
    { x: 0, y: 0 },
    { x: 10, y: 100 },
  ];
  it('interpolates linearly between anchors', () => {
    expect(interpolate(anchors, 5)).toBe(50);
    expect(interpolate(anchors, 2.5)).toBe(25);
  });
  it('clamps below the first and above the last anchor', () => {
    expect(interpolate(anchors, -5)).toBe(0);
    expect(interpolate(anchors, 999)).toBe(100);
  });
  it('handles a single anchor and empty input', () => {
    expect(interpolate([{ x: 1, y: 42 }], 5)).toBe(42);
    expect(interpolate([], 5)).toBe(0);
  });
});

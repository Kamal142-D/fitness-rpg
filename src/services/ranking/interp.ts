/**
 * Piecewise-linear interpolation over sorted anchor points, clamped at the ends.
 * Used to map a raw metric (e.g. a bodyweight-strength ratio) onto the 0..100
 * score scale via provisional anchor tables.
 */
export interface Anchor {
  x: number;
  y: number;
}

/** Interpolate y for a given x across anchors sorted ascending by x. */
export function interpolate(anchors: readonly Anchor[], x: number): number {
  if (anchors.length === 0) return 0;
  if (x <= anchors[0].x) return anchors[0].y;
  const last = anchors[anchors.length - 1];
  if (x >= last.x) return last.y;
  for (let i = 0; i < anchors.length - 1; i++) {
    const a = anchors[i];
    const b = anchors[i + 1];
    if (x >= a.x && x <= b.x) {
      const t = (x - a.x) / (b.x - a.x);
      return a.y + t * (b.y - a.y);
    }
  }
  return last.y;
}

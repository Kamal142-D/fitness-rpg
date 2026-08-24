/**
 * Unit conversion. Canonical storage is KILOGRAMS (PLAN.txt §4); these helpers
 * support a future imperial display option. Pure.
 */
const LB_PER_KG = 2.2046226218;

export function kgToLb(kg: number): number {
  return kg * LB_PER_KG;
}

export function lbToKg(lb: number): number {
  return lb / LB_PER_KG;
}

/** Round to `decimals` places (default 1). */
export function roundTo(value: number, decimals = 1): number {
  const f = 10 ** decimals;
  return Math.round(value * f) / f;
}

/**
 * Small color helpers. Pure and dependency-free.
 */

/**
 * Convert a `#RRGGBB` hex string to an `rgba(...)` string at the given alpha.
 * Falls back to the input if it is not a 6-digit hex (so callers never crash on
 * an already-rgba value).
 */
export function hexToRgba(hex: string, alpha: number): string {
  const match = /^#([0-9a-fA-F]{6})$/.exec(hex);
  if (!match) return hex;
  const int = parseInt(match[1], 16);
  const r = (int >> 16) & 255;
  const g = (int >> 8) & 255;
  const b = int & 255;
  const a = Math.min(1, Math.max(0, alpha));
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}

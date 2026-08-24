/**
 * Estimated 1RM via the Epley formula (PLAN.txt §6.2): weight * (1 + reps / 30).
 *
 * Computed at log time for convenience and stored per set. The ranking ENGINE
 * (Phase 9) decides which sets qualify; this helper only does the arithmetic and
 * guards obviously invalid input. Only sensible for conventional loaded reps in
 * the 1..12 range — returns null otherwise so callers don't store noise.
 */
export function estimatedOneRepMax(weightKg: number | null, reps: number | null): number | null {
  if (weightKg == null || reps == null) return null;
  if (weightKg <= 0 || reps < 1 || reps > 12) return null;
  if (reps === 1) return round2(weightKg);
  return round2(weightKg * (1 + reps / 30));
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

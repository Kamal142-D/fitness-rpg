/**
 * Rest-timer math. Kept pure and time-injectable so it survives app restarts:
 * the store persists an absolute `restEndsAt` epoch and the remaining time is
 * always derived from the current clock, never a decrementing counter.
 */
export function restRemainingSeconds(restEndsAt: number | null, now: number = Date.now()): number {
  if (restEndsAt == null) return 0;
  const ms = restEndsAt - now;
  return ms <= 0 ? 0 : Math.ceil(ms / 1000);
}

export function isResting(restEndsAt: number | null, now: number = Date.now()): boolean {
  return restRemainingSeconds(restEndsAt, now) > 0;
}

/** Format seconds as M:SS. */
export function formatClock(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const m = Math.floor(s / 60);
  const rem = s % 60;
  return `${m}:${rem.toString().padStart(2, '0')}`;
}

/**
 * Semantic-ish version comparison. Pure. Tolerates a leading "v" and
 * differing part counts (e.g. "1.2" vs "1.2.0"). Non-numeric/build suffixes
 * are ignored.
 */
export function parseVersion(v: string): number[] {
  const cleaned = v.trim().replace(/^v/i, '');
  const core = cleaned.split(/[-+]/)[0]; // drop prerelease/build metadata
  return core
    .split('.')
    .map((p) => parseInt(p, 10))
    .map((n) => (Number.isFinite(n) ? n : 0));
}

/** -1 if a<b, 0 if equal, 1 if a>b. */
export function compareVersions(a: string, b: string): -1 | 0 | 1 {
  const pa = parseVersion(a);
  const pb = parseVersion(b);
  const len = Math.max(pa.length, pb.length);
  for (let i = 0; i < len; i++) {
    const x = pa[i] ?? 0;
    const y = pb[i] ?? 0;
    if (x > y) return 1;
    if (x < y) return -1;
  }
  return 0;
}

/** True when `latest` is strictly newer than `current`. */
export function isNewerVersion(latest: string, current: string): boolean {
  return compareVersions(latest, current) > 0;
}

import {
  RANK_THRESHOLDS,
  RANKS,
  clampScore,
  getRankColor,
  scoreToRank,
  type Rank,
} from '@/constants/ranks';

describe('clampScore', () => {
  it('passes through in-range values', () => {
    expect(clampScore(0)).toBe(0);
    expect(clampScore(50)).toBe(50);
    expect(clampScore(100)).toBe(100);
  });

  it('clamps below 0 and above 100', () => {
    expect(clampScore(-25)).toBe(0);
    expect(clampScore(999)).toBe(100);
  });

  it('treats NaN as 0', () => {
    expect(clampScore(Number.NaN)).toBe(0);
  });
});

describe('scoreToRank', () => {
  it('maps the documented band midpoints (PLAN.txt §6.1)', () => {
    expect(scoreToRank(10)).toBe('E');
    expect(scoreToRank(27)).toBe('D');
    expect(scoreToRank(42)).toBe('C');
    expect(scoreToRank(57)).toBe('B');
    expect(scoreToRank(72)).toBe('A');
    expect(scoreToRank(90)).toBe('S');
  });

  it('is correct at every band boundary (min and max, inclusive)', () => {
    for (const band of RANK_THRESHOLDS) {
      expect(scoreToRank(band.min)).toBe(band.rank);
      expect(scoreToRank(band.max)).toBe(band.rank);
    }
  });

  it('is correct one point either side of each internal boundary', () => {
    // The seam between two adjacent bands: max of lower vs min of upper.
    for (let i = 0; i < RANK_THRESHOLDS.length - 1; i++) {
      const lower = RANK_THRESHOLDS[i];
      const upper = RANK_THRESHOLDS[i + 1];
      expect(upper.min).toBe(lower.max + 1); // bands are contiguous, no gaps
      expect(scoreToRank(lower.max)).toBe(lower.rank);
      expect(scoreToRank(upper.min)).toBe(upper.rank);
    }
  });

  it('clamps out-of-range scores to the end ranks', () => {
    expect(scoreToRank(-50)).toBe('E');
    expect(scoreToRank(150)).toBe('S');
  });

  it('never throws and always returns a known rank for arbitrary input', () => {
    const samples = [Number.NaN, -1, 0, 19.5, 34.9, 65, 79.999, 80, 100, 100.1];
    for (const s of samples) {
      expect(RANKS).toContain(scoreToRank(s));
    }
  });
});

describe('rank threshold table integrity', () => {
  it('covers 0..100 contiguously with no gaps or overlaps', () => {
    expect(RANK_THRESHOLDS[0].min).toBe(0);
    expect(RANK_THRESHOLDS[RANK_THRESHOLDS.length - 1].max).toBe(100);
    for (let i = 0; i < RANK_THRESHOLDS.length - 1; i++) {
      expect(RANK_THRESHOLDS[i + 1].min).toBe(RANK_THRESHOLDS[i].max + 1);
    }
  });

  it('lists bands in weakest-to-strongest order matching RANKS', () => {
    expect(RANK_THRESHOLDS.map((b) => b.rank)).toEqual([...RANKS]);
  });
});

describe('getRankColor', () => {
  it('returns a hex color for every rank', () => {
    for (const rank of RANKS as readonly Rank[]) {
      expect(getRankColor(rank)).toMatch(/^#[0-9A-Fa-f]{6}$/);
    }
  });
});

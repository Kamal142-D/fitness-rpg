import { applyXp, xpForWorkout } from '@/features/progression/rewards';

describe('xpForWorkout', () => {
  it('sums base, per-set, PR, and gate-clear bonuses', () => {
    // 300 + 8*10 + 1*50 + 200 (B bonus) = 630
    expect(
      xpForWorkout({
        completed: true,
        validWorkingSets: 8,
        meaningfulPrCount: 1,
        gateClearRank: 'B',
      }),
    ).toBe(630);
  });
  it('awards nothing for an incomplete, empty workout', () => {
    expect(
      xpForWorkout({
        completed: false,
        validWorkingSets: 0,
        meaningfulPrCount: 0,
        gateClearRank: null,
      }),
    ).toBe(0);
  });
  it('uses the S gate-clear bonus of 300', () => {
    expect(
      xpForWorkout({
        completed: true,
        validWorkingSets: 0,
        meaningfulPrCount: 0,
        gateClearRank: 'S',
      }),
    ).toBe(600);
  });
});

describe('applyXp', () => {
  it('levels up once when XP crosses the requirement', () => {
    // L1 requires 100; 300 earned -> level 2 with 200 carried over.
    const r = applyXp({ level: 1, currentXp: 0, lifetimeXp: 0 }, 300);
    expect(r).toMatchObject({
      level: 2,
      currentXp: 200,
      lifetimeXp: 300,
      leveledUp: true,
      levelsGained: 1,
    });
  });
  it('rolls over multiple levels for a large gain', () => {
    const r = applyXp({ level: 1, currentXp: 0, lifetimeXp: 0 }, 100_000);
    expect(r.levelsGained).toBeGreaterThan(1);
    expect(r.lifetimeXp).toBe(100_000);
  });
  it('stays in-level for a small gain', () => {
    const r = applyXp({ level: 1, currentXp: 0, lifetimeXp: 0 }, 50);
    expect(r).toMatchObject({ level: 1, currentXp: 50, leveledUp: false, levelsGained: 0 });
  });
  it('is a no-op for zero XP', () => {
    const r = applyXp({ level: 3, currentXp: 40, lifetimeXp: 900 }, 0);
    expect(r).toMatchObject({ level: 3, currentXp: 40, leveledUp: false });
  });
});

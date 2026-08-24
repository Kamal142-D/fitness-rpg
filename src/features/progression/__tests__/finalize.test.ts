import { buildProgressionUpdate } from '@/features/progression/finalize';

describe('buildProgressionUpdate', () => {
  it('applies XP (with level-up), carries null attributes, and computes Hunter rank', () => {
    const out = buildProgressionUpdate({
      current: { level: 1, currentXp: 0, lifetimeXp: 0 },
      currentAttributes: { strength: 0, physique: 0, endurance: 0, discipline: 0 },
      xpEarned: 300,
      streak: { current: 3, longest: 5 },
      attributes: { strength: 70, physique: null, endurance: null, discipline: 80 },
    });

    // XP: level 2, 200 carried, 300 lifetime.
    expect(out.level).toBe(2);
    expect(out.current_xp).toBe(200);
    expect(out.lifetime_xp).toBe(300);

    // Attributes carried / set.
    expect(out.strength_score).toBe(70);
    expect(out.discipline_score).toBe(80);
    expect(out.physique_score).toBe(0);

    // Hunter blend over meaningful attributes only (strength .40 + discipline .15):
    // (70*.4 + 80*.15) / .55 = 40 / .55 ≈ 72.7 -> A
    expect(out.hunter_score).toBeCloseTo(72.7, 1);
    expect(out.hunter_rank).toBe('A');
    expect(out.current_streak_days).toBe(3);
  });

  it('carries current attributes when none are recomputed, and stays neutral when all zero', () => {
    const out = buildProgressionUpdate({
      current: { level: 5, currentXp: 10, lifetimeXp: 999 },
      currentAttributes: { strength: 0, physique: 0, endurance: 0, discipline: 0 },
      xpEarned: 0,
      streak: { current: 0, longest: 2 },
      attributes: { strength: null, physique: null, endurance: null, discipline: null },
    });
    expect(out.level).toBe(5);
    expect(out.hunter_score).toBe(60); // all-zero -> neutral
    expect(out.hunter_rank).toBe('B');
  });
});

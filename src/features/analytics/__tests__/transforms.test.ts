import {
  computeExerciseRanks,
  frequencyByWeek,
  monthlyComparison,
  startOfWeekMs,
  volumeByWeek,
} from '@/features/analytics/transforms';
import type { SessionSummary } from '@/features/analytics/types';

// Fixed reference: Wed 2026-08-26 12:00 local. Week starts Mon 2026-08-24.
const NOW = new Date(2026, 7, 26, 12).getTime();

function session(daysAgo: number, volumeKg: number): SessionSummary {
  return {
    id: `s${daysAgo}`,
    name: 'W',
    completedAt: new Date(NOW - daysAgo * 86_400_000).toISOString(),
    gateClearRank: null,
    totalVolumeKg: volumeKg,
    durationSeconds: 0,
  };
}

describe('startOfWeekMs', () => {
  it('snaps to the Monday of the week', () => {
    const mon = new Date(startOfWeekMs(NOW));
    expect(mon.getDay()).toBe(1); // Monday
    expect(mon.getDate()).toBe(24);
  });
});

describe('volumeByWeek', () => {
  it('buckets volume per week and pads empty weeks', () => {
    const sessions = [session(0, 1000), session(8, 500)]; // this week, prev week
    const out = volumeByWeek(sessions, 3, NOW);
    expect(out).toHaveLength(3);
    expect(out.map((p) => p.value)).toEqual([0, 500, 1000]);
  });
  it('excludes sessions older than the window', () => {
    const out = volumeByWeek([session(60, 9999)], 3, NOW);
    expect(out.every((p) => p.value === 0)).toBe(true);
  });
});

describe('frequencyByWeek', () => {
  it('counts workouts per week', () => {
    const out = frequencyByWeek([session(0, 100), session(1, 100), session(8, 100)], 2, NOW);
    expect(out.map((p) => p.value)).toEqual([1, 2]);
  });
});

describe('monthlyComparison', () => {
  it('splits this month vs last month', () => {
    const sessions = [
      session(1, 1000), // Aug (this month)
      {
        ...session(0, 0),
        completedAt: new Date(2026, 6, 15, 12).toISOString(),
        totalVolumeKg: 2000,
      }, // Jul
      {
        ...session(0, 0),
        completedAt: new Date(2026, 5, 10, 12).toISOString(),
        totalVolumeKg: 5000,
      }, // Jun (neither)
    ];
    const c = monthlyComparison(sessions, NOW);
    expect(c.thisMonth).toEqual({ workouts: 1, volumeKg: 1000 });
    expect(c.lastMonth).toEqual({ workouts: 1, volumeKg: 2000 });
  });
});

describe('computeExerciseRanks', () => {
  it('ranks scorable lifts, drops unranked, sorts by score desc', () => {
    const ranks = computeExerciseRanks(
      [
        { exerciseId: 'e1', name: 'Barbell Bench Press', best1RMkg: 80 }, // ratio 1.0 -> 50 (B)
        { exerciseId: 'e2', name: 'Lateral Raise', best1RMkg: 30 }, // no standard -> dropped
        { exerciseId: 'e3', name: 'Barbell Back Squat', best1RMkg: 160 }, // ratio 2.0 -> ~86 (S)
      ],
      80,
      'male',
    );
    expect(ranks.map((r) => r.name)).toEqual(['Barbell Back Squat', 'Barbell Bench Press']);
    expect(ranks[1].rank).toBe('B');
    expect(ranks[0].rank).toBe('S');
  });
});

import { formatClock, isResting, restRemainingSeconds } from '@/features/workouts/restTimer';

describe('restRemainingSeconds', () => {
  it('is 0 when there is no rest end', () => {
    expect(restRemainingSeconds(null, 1000)).toBe(0);
  });
  it('rounds up remaining time and floors at 0', () => {
    expect(restRemainingSeconds(10_500, 0)).toBe(11);
    expect(restRemainingSeconds(500, 1000)).toBe(0);
  });
  it('isResting reflects remaining > 0', () => {
    expect(isResting(5000, 0)).toBe(true);
    expect(isResting(0, 1000)).toBe(false);
  });
});

describe('formatClock', () => {
  it('formats seconds as M:SS', () => {
    expect(formatClock(0)).toBe('0:00');
    expect(formatClock(9)).toBe('0:09');
    expect(formatClock(75)).toBe('1:15');
    expect(formatClock(600)).toBe('10:00');
  });
});

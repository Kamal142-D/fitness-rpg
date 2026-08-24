/**
 * Pure analytics transforms for the Player screen. Time is injected (`now`) so
 * the bucketing is deterministic and testable.
 */
import type { Sex } from '@/services/ranking/config';
import { exerciseScore, permanentExerciseRank } from '@/services/ranking';
import type {
  ExerciseRankItem,
  ExerciseStatInput,
  MonthlyComparison,
  SeriesPoint,
  SessionSummary,
} from '@/features/analytics/types';

const DAY_MS = 86_400_000;

/** Local midnight of the Monday that starts this date's week. */
export function startOfWeekMs(ms: number): number {
  const d = new Date(ms);
  d.setHours(0, 0, 0, 0);
  const dow = (d.getDay() + 6) % 7; // Monday = 0
  d.setDate(d.getDate() - dow);
  return d.getTime();
}

function weekLabel(ms: number): string {
  const d = new Date(ms);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

function bucketByWeek(
  sessions: SessionSummary[],
  weeks: number,
  now: number,
  value: (s: SessionSummary) => number,
): SeriesPoint[] {
  const totals = new Map<number, number>();
  for (const s of sessions) {
    if (!s.completedAt) continue;
    const ws = startOfWeekMs(Date.parse(s.completedAt));
    totals.set(ws, (totals.get(ws) ?? 0) + value(s));
  }
  const thisWeek = startOfWeekMs(now);
  const out: SeriesPoint[] = [];
  for (let i = weeks - 1; i >= 0; i--) {
    const ws = startOfWeekMs(thisWeek - i * 7 * DAY_MS);
    out.push({ label: weekLabel(ws), value: Math.round(totals.get(ws) ?? 0) });
  }
  return out;
}

/** Total training volume (kg) per week, over the last `weeks` weeks. */
export function volumeByWeek(
  sessions: SessionSummary[],
  weeks = 8,
  now = Date.now(),
): SeriesPoint[] {
  return bucketByWeek(sessions, weeks, now, (s) => s.totalVolumeKg ?? 0);
}

/** Workout count per week, over the last `weeks` weeks. */
export function frequencyByWeek(
  sessions: SessionSummary[],
  weeks = 8,
  now = Date.now(),
): SeriesPoint[] {
  return bucketByWeek(sessions, weeks, now, () => 1);
}

/** This calendar month vs last, in workouts + volume. */
export function monthlyComparison(sessions: SessionSummary[], now = Date.now()): MonthlyComparison {
  const ref = new Date(now);
  const thisY = ref.getFullYear();
  const thisM = ref.getMonth();
  const last = new Date(thisY, thisM - 1, 1);
  const lastY = last.getFullYear();
  const lastM = last.getMonth();

  const acc = { thisMonth: { workouts: 0, volumeKg: 0 }, lastMonth: { workouts: 0, volumeKg: 0 } };
  for (const s of sessions) {
    if (!s.completedAt) continue;
    const d = new Date(Date.parse(s.completedAt));
    const vol = s.totalVolumeKg ?? 0;
    if (d.getFullYear() === thisY && d.getMonth() === thisM) {
      acc.thisMonth.workouts += 1;
      acc.thisMonth.volumeKg += vol;
    } else if (d.getFullYear() === lastY && d.getMonth() === lastM) {
      acc.lastMonth.workouts += 1;
      acc.lastMonth.volumeKg += vol;
    }
  }
  acc.thisMonth.volumeKg = Math.round(acc.thisMonth.volumeKg);
  acc.lastMonth.volumeKg = Math.round(acc.lastMonth.volumeKg);
  return acc;
}

/**
 * Compute permanent Exercise Ranks client-side from stored bests + profile,
 * highest score first. Exercises without a strength standard are dropped.
 */
export function computeExerciseRanks(
  stats: ExerciseStatInput[],
  bodyweightKg: number | null,
  sex: Sex,
): ExerciseRankItem[] {
  const items: ExerciseRankItem[] = [];
  for (const s of stats) {
    const score = exerciseScore({
      exerciseName: s.name,
      bestEstimated1rmKg: s.best1RMkg,
      bodyweightKg,
      sex,
    });
    if (score == null) continue;
    items.push({
      exerciseId: s.exerciseId,
      name: s.name,
      rank: permanentExerciseRank(score),
      score,
      best1RMkg: s.best1RMkg,
    });
  }
  return items.sort((a, b) => b.score - a.score);
}

/**
 * Transform the active workout into the atomic `complete_workout` RPC payload,
 * plus summary aggregates for the completion screen. Pure and testable.
 *
 * Only completed sets are persisted, and only exercises that have at least one
 * completed set. Set numbers and exercise order are re-sequenced so they satisfy
 * the DB's uniqueness constraints regardless of what was skipped.
 */
import { estimatedOneRepMax } from '@/features/workouts/epley';
import type { ActiveWorkout } from '@/features/workouts/types';

export interface CompletionSetPayload {
  set_number: number;
  weight_kg: number | null;
  reps: number | null;
  rpe: number | null;
  is_warmup: boolean;
  is_completed: boolean;
  estimated_1rm_kg: number | null;
  completed_at: string | null;
}

export interface CompletionExercisePayload {
  exercise_id: string;
  order_index: number;
  notes: string | null;
  /** Filled by the ranking step (gateResult) before completion; null otherwise. */
  exercise_score: number | null;
  performance_grade: string | null;
  sets: CompletionSetPayload[];
}

export interface CompletionSession {
  id: string;
  template_id: string | null;
  name: string;
  gate_difficulty: string | null;
  started_at: string;
  completed_at: string;
  duration_seconds: number;
  total_volume_kg: number;
  /** Gate scoring fields, filled by the ranking step; null until then. */
  completion_score: number | null;
  progress_score: number | null;
  quality_score: number | null;
  gate_score: number | null;
  gate_clear_rank: string | null;
  xp_earned: number | null;
}

export interface CompletionPayload {
  session: CompletionSession;
  exercises: CompletionExercisePayload[];
}

export interface CompletionAggregates {
  name: string;
  gateDifficulty: string | null;
  durationSeconds: number;
  totalVolumeKg: number;
  completedSets: number;
  /** Non-warm-up sets that were planned (completed or not) — for completion %. */
  plannedWorkingSets: number;
  exerciseCount: number;
}

export interface CompletionResult {
  payload: CompletionPayload;
  aggregates: CompletionAggregates;
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

export function buildCompletionPayload(
  state: ActiveWorkout,
  now: number = Date.now(),
): CompletionResult {
  const completedAt = new Date(now).toISOString();
  const durationSeconds = Math.max(0, Math.floor((now - Date.parse(state.startedAt)) / 1000));

  let totalVolume = 0;
  let completedSets = 0;
  let plannedWorkingSets = 0;

  const exercises: CompletionExercisePayload[] = [];
  for (const ex of state.exercises) {
    plannedWorkingSets += ex.sets.filter((s) => !s.isWarmup).length;
    const done = ex.sets.filter((s) => s.isCompleted);
    if (done.length === 0) continue;

    const sets: CompletionSetPayload[] = done.map((s, i) => {
      if (!s.isWarmup) {
        completedSets += 1;
        totalVolume += (s.weightKg ?? 0) * (s.reps ?? 0);
      }
      return {
        set_number: i + 1,
        weight_kg: s.weightKg,
        reps: s.reps,
        rpe: s.rpe,
        is_warmup: s.isWarmup,
        is_completed: true,
        estimated_1rm_kg: s.isWarmup ? null : estimatedOneRepMax(s.weightKg, s.reps),
        completed_at: s.completedAt,
      };
    });

    exercises.push({
      exercise_id: ex.exerciseId,
      order_index: exercises.length,
      notes: ex.notes.trim() ? ex.notes.trim() : null,
      exercise_score: null,
      performance_grade: null,
      sets,
    });
  }

  const totalVolumeKg = round2(totalVolume);

  return {
    payload: {
      session: {
        id: state.sessionId,
        template_id: state.templateId,
        name: state.name,
        gate_difficulty: state.gateDifficulty,
        started_at: state.startedAt,
        completed_at: completedAt,
        duration_seconds: durationSeconds,
        total_volume_kg: totalVolumeKg,
        completion_score: null,
        progress_score: null,
        quality_score: null,
        gate_score: null,
        gate_clear_rank: null,
        xp_earned: null,
      },
      exercises,
    },
    aggregates: {
      name: state.name,
      gateDifficulty: state.gateDifficulty,
      durationSeconds,
      totalVolumeKg,
      completedSets,
      plannedWorkingSets,
      exerciseCount: exercises.length,
    },
  };
}

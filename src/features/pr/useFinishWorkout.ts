import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import { applyWorkoutResults, getExerciseStats } from '@/features/pr/api';
import { detectPRs, prioritizePRs } from '@/features/pr/detect';
import type { DetectExercise, DetectedPR, PriorStat } from '@/features/pr/types';
import {
  applySessionProgression,
  buildProgressionUpdate,
  computeAttributes,
  getFinishInputs,
  getProgression,
} from '@/features/progression';
import { recordWorkoutForQuests } from '@/features/quests';
import { completeWorkout } from '@/features/workouts/api';
import { computeGateResult, type GateResult } from '@/features/workouts/gateResult';
import type { CompletionAggregates, CompletionPayload } from '@/features/workouts/payload';
import { updateStreak } from '@/services/ranking';

export interface FinishWorkoutInput {
  payload: CompletionPayload;
  aggregates: CompletionAggregates;
}

export interface FinishWorkoutResult {
  sessionId: string;
  aggregates: CompletionAggregates;
  prs: DetectedPR[];
  gate: GateResult;
}

function toDetectExercises(payload: CompletionPayload): DetectExercise[] {
  return payload.exercises.map((ex) => ({
    exerciseId: ex.exercise_id,
    orderIndex: ex.order_index,
    sets: ex.sets.map((s) => ({
      setNumber: s.set_number,
      weightKg: s.weight_kg,
      reps: s.reps,
      est1RM: s.estimated_1rm_kg,
      isWarmup: s.is_warmup,
    })),
  }));
}

/** Attach the computed Gate result to the session + per-exercise payload. */
function augment(payload: CompletionPayload, gate: GateResult): CompletionPayload {
  return {
    session: {
      ...payload.session,
      completion_score: gate.completionScore,
      progress_score: gate.progressScore,
      quality_score: gate.qualityScore,
      gate_score: gate.gateScore,
      gate_clear_rank: gate.gateClearRank,
      xp_earned: gate.xpEarned,
    },
    exercises: payload.exercises.map((ex, i) => ({
      ...ex,
      exercise_score: gate.perExercise[i]?.performanceScore ?? null,
      performance_grade: gate.perExercise[i]?.performanceGrade ?? null,
    })),
  };
}

/**
 * Finish a workout end-to-end: fetch prior bests, detect PRs, compute the Gate
 * result (all pure), persist the session with those results (atomic RPC), then
 * best-effort apply PRs/stat updates. The workout is saved once completion
 * succeeds; PR application never blocks the result.
 */
export function useFinishWorkout() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation<FinishWorkoutResult, Error, FinishWorkoutInput>({
    mutationFn: async ({ payload, aggregates }) => {
      const exerciseIds = [...new Set(payload.exercises.map((e) => e.exercise_id))];

      // Prior bests power PR detection AND the performance baseline. If the fetch
      // fails, degrade to no history (neutral) rather than failing the finish.
      let priorStats: Record<string, PriorStat | undefined> = {};
      try {
        priorStats = await getExerciseStats(exerciseIds);
      } catch {
        priorStats = {};
      }

      const detection = detectPRs(toDetectExercises(payload), priorStats);
      const gate = computeGateResult(payload, priorStats, aggregates, detection.prs);

      const sessionId = await completeWorkout(augment(payload, gate));

      try {
        await applyWorkoutResults(sessionId, detection.prs, detection.stats);
      } catch {
        // Workout + gate result are saved; PR persistence is best-effort.
      }

      // Quest progress + durable progression (XP/level/streak/attributes). Each
      // is idempotent server-side and best-effort — never blocks the result.
      try {
        await recordWorkoutForQuests(sessionId);
      } catch {
        /* best-effort */
      }
      try {
        const uid = user?.id;
        const prog = uid ? await getProgression(uid) : null;
        if (uid && prog) {
          const inputs = await getFinishInputs(uid);
          const newStreak = updateStreak(
            { current: prog.current_streak_days, longest: prog.longest_streak_days },
            { didTrain: true, isScheduledRest: false },
          );
          const snapshot = buildProgressionUpdate({
            current: {
              level: prog.level,
              currentXp: prog.current_xp,
              lifetimeXp: prog.lifetime_xp,
            },
            currentAttributes: {
              strength: prog.strength_score,
              physique: prog.physique_score,
              endurance: prog.endurance_score,
              discipline: prog.discipline_score,
            },
            xpEarned: gate.xpEarned,
            streak: newStreak,
            attributes: computeAttributes(inputs, newStreak.current),
          });
          await applySessionProgression(sessionId, snapshot);
        }
      } catch {
        /* best-effort */
      }

      return { sessionId, aggregates, prs: prioritizePRs(detection.prs), gate };
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['progression', user?.id] });
      void queryClient.invalidateQueries({ queryKey: ['quests', user?.id] });
      void queryClient.invalidateQueries({ queryKey: ['workout-history', user?.id] });
      void queryClient.invalidateQueries({ queryKey: ['personal-records', user?.id] });
    },
  });
}

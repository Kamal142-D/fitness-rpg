import type { Rank } from '@/constants/ranks';

export interface SessionSummary {
  id: string;
  name: string | null;
  completedAt: string | null;
  gateClearRank: string | null;
  totalVolumeKg: number | null;
  durationSeconds: number | null;
}

export interface ExerciseStatInput {
  exerciseId: string;
  name: string;
  best1RMkg: number | null;
}

export interface ExerciseRankItem {
  exerciseId: string;
  name: string;
  rank: Rank;
  score: number;
  best1RMkg: number | null;
}

export interface PrHistoryItem {
  id: string;
  exerciseName: string;
  recordType: string;
  newValue: number;
  achievedAt: string;
}

export interface WeightPoint {
  date: string;
  weightKg: number;
}

export interface SeriesPoint {
  label: string;
  value: number;
}

export interface MonthTotals {
  workouts: number;
  volumeKg: number;
}

export interface MonthlyComparison {
  thisMonth: MonthTotals;
  lastMonth: MonthTotals;
}

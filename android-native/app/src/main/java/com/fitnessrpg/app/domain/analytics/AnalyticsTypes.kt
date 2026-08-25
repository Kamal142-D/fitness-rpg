package com.fitnessrpg.app.domain.analytics

import com.fitnessrpg.app.domain.rank.Rank

data class SessionSummary(
    val id: String,
    val name: String?,
    val completedAt: String?,
    val gateClearRank: String?,
    val totalVolumeKg: Double?,
    val durationSeconds: Int?,
)

data class ExerciseStatInput(
    val exerciseId: String,
    val name: String,
    val best1RMkg: Double?,
)

data class ExerciseRankItem(
    val exerciseId: String,
    val name: String,
    val rank: Rank,
    val score: Double,
    val best1RMkg: Double?,
)

data class PrHistoryItem(
    val id: String,
    val exerciseName: String,
    val recordType: String,
    val newValue: Double,
    val achievedAt: String,
)

data class WeightPoint(val date: String, val weightKg: Double)

data class SeriesPoint(val label: String, val value: Int)

data class MonthTotals(val workouts: Int, val volumeKg: Int)

data class MonthlyComparison(val thisMonth: MonthTotals, val lastMonth: MonthTotals)

/** Everything the Player screen needs, fetched in one pass. */
data class PlayerData(
    val sessions: List<SessionSummary>,
    val stats: List<ExerciseStatInput>,
    val bodyweightKg: Double?,
    val sex: String?,
    val prs: List<PrHistoryItem>,
    val weights: List<WeightPoint>,
)

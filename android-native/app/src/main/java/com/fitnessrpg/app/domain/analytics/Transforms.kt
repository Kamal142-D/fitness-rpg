package com.fitnessrpg.app.domain.analytics

import com.fitnessrpg.app.domain.ranking.ExerciseScoreInput
import com.fitnessrpg.app.domain.ranking.exerciseScore
import com.fitnessrpg.app.domain.ranking.permanentExerciseRank
import com.fitnessrpg.app.util.millisFromIso
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Pure analytics transforms for the Player screen. Time is injected ([now]) so
 * the bucketing is deterministic and testable. Local-time semantics match the
 * original (system default zone).
 */
private const val DAY_MS = 86_400_000L
private val zone: ZoneId get() = ZoneId.systemDefault()

/** Local midnight of the Monday that starts this date's week. */
fun startOfWeekMs(ms: Long): Long {
    val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val daysFromMonday = (date.dayOfWeek.value - 1).toLong() // Monday = 0 .. Sunday = 6
    val monday = date.minusDays(daysFromMonday)
    return monday.atStartOfDay(zone).toInstant().toEpochMilli()
}

private fun weekLabel(ms: Long): String {
    val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    return "${d.monthValue}/${d.dayOfMonth}"
}

private fun bucketByWeek(
    sessions: List<SessionSummary>,
    weeks: Int,
    now: Long,
    value: (SessionSummary) -> Double,
): List<SeriesPoint> {
    val totals = HashMap<Long, Double>()
    for (s in sessions) {
        val ca = s.completedAt ?: continue
        val ws = startOfWeekMs(millisFromIso(ca))
        totals[ws] = (totals[ws] ?: 0.0) + value(s)
    }
    val thisWeek = startOfWeekMs(now)
    val out = mutableListOf<SeriesPoint>()
    for (i in weeks - 1 downTo 0) {
        val ws = startOfWeekMs(thisWeek - i * 7 * DAY_MS)
        out.add(SeriesPoint(weekLabel(ws), (totals[ws] ?: 0.0).roundToInt()))
    }
    return out
}

/** Total training volume (kg) per week, over the last [weeks] weeks. */
fun volumeByWeek(
    sessions: List<SessionSummary>,
    weeks: Int = 8,
    now: Long = System.currentTimeMillis(),
): List<SeriesPoint> = bucketByWeek(sessions, weeks, now) { it.totalVolumeKg ?: 0.0 }

/** Workout count per week, over the last [weeks] weeks. */
fun frequencyByWeek(
    sessions: List<SessionSummary>,
    weeks: Int = 8,
    now: Long = System.currentTimeMillis(),
): List<SeriesPoint> = bucketByWeek(sessions, weeks, now) { 1.0 }

/** This calendar month vs last, in workouts + volume. */
fun monthlyComparison(
    sessions: List<SessionSummary>,
    now: Long = System.currentTimeMillis(),
): MonthlyComparison {
    val ref = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val thisY = ref.year
    val thisM = ref.monthValue
    val lastDate = ref.withDayOfMonth(1).minusMonths(1)
    val lastY = lastDate.year
    val lastM = lastDate.monthValue

    var tmWorkouts = 0
    var tmVolume = 0.0
    var lmWorkouts = 0
    var lmVolume = 0.0
    for (s in sessions) {
        val ca = s.completedAt ?: continue
        val d = Instant.ofEpochMilli(millisFromIso(ca)).atZone(zone).toLocalDate()
        val vol = s.totalVolumeKg ?: 0.0
        if (d.year == thisY && d.monthValue == thisM) {
            tmWorkouts += 1
            tmVolume += vol
        } else if (d.year == lastY && d.monthValue == lastM) {
            lmWorkouts += 1
            lmVolume += vol
        }
    }
    return MonthlyComparison(
        MonthTotals(tmWorkouts, tmVolume.roundToInt()),
        MonthTotals(lmWorkouts, lmVolume.roundToInt()),
    )
}

/**
 * Compute permanent Exercise Ranks from stored bests + profile, highest score
 * first. Exercises without a strength standard are dropped.
 */
fun computeExerciseRanks(
    stats: List<ExerciseStatInput>,
    bodyweightKg: Double?,
    sex: String?,
): List<ExerciseRankItem> {
    val items = mutableListOf<ExerciseRankItem>()
    for (s in stats) {
        val score = exerciseScore(ExerciseScoreInput(s.name, s.best1RMkg, bodyweightKg, sex)) ?: continue
        items.add(ExerciseRankItem(s.exerciseId, s.name, permanentExerciseRank(score), score, s.best1RMkg))
    }
    return items.sortedByDescending { it.score }
}

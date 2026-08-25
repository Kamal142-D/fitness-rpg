package com.fitnessrpg.app.domain.analytics

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.util.isoFromMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class TransformsTest {

    // Fixed reference: Wed 2026-08-26 12:00 local. Week starts Mon 2026-08-24.
    private val now: Long =
        LocalDateTime.of(2026, 8, 26, 12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun session(daysAgo: Int, volumeKg: Double) = SessionSummary(
        id = "s$daysAgo",
        name = "W",
        completedAt = isoFromMillis(now - daysAgo * 86_400_000L),
        gateClearRank = null,
        totalVolumeKg = volumeKg,
        durationSeconds = 0,
    )

    private fun atNoon(y: Int, m: Int, d: Int): Long =
        LocalDateTime.of(y, m, d, 12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `startOfWeekMs snaps to the Monday of the week`() {
        val monday = Instant.ofEpochMilli(startOfWeekMs(now)).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(24, monday.dayOfMonth)
    }

    @Test
    fun `volumeByWeek buckets volume per week and pads empty weeks`() {
        val sessions = listOf(session(0, 1000.0), session(8, 500.0))
        val out = volumeByWeek(sessions, 3, now)
        assertEquals(3, out.size)
        assertEquals(listOf(0, 500, 1000), out.map { it.value })
    }

    @Test
    fun `volumeByWeek excludes sessions older than the window`() {
        val out = volumeByWeek(listOf(session(60, 9999.0)), 3, now)
        assertTrue(out.all { it.value == 0 })
    }

    @Test
    fun `frequencyByWeek counts workouts per week`() {
        val out = frequencyByWeek(listOf(session(0, 100.0), session(1, 100.0), session(8, 100.0)), 2, now)
        assertEquals(listOf(1, 2), out.map { it.value })
    }

    @Test
    fun `monthlyComparison splits this month vs last month`() {
        val sessions = listOf(
            session(1, 1000.0), // Aug (this month)
            SessionSummary("jul", "W", isoFromMillis(atNoon(2026, 7, 15)), null, 2000.0, 0),
            SessionSummary("jun", "W", isoFromMillis(atNoon(2026, 6, 10)), null, 5000.0, 0),
        )
        val c = monthlyComparison(sessions, now)
        assertEquals(MonthTotals(1, 1000), c.thisMonth)
        assertEquals(MonthTotals(1, 2000), c.lastMonth)
    }

    @Test
    fun `computeExerciseRanks ranks scorable lifts, drops unranked, sorts by score desc`() {
        val ranks = computeExerciseRanks(
            listOf(
                ExerciseStatInput("e1", "Barbell Bench Press", 80.0), // ratio 1.0 -> 50 (B)
                ExerciseStatInput("e2", "Lateral Raise", 30.0), // no standard -> dropped
                ExerciseStatInput("e3", "Barbell Back Squat", 160.0), // ratio 2.0 -> ~86 (S)
            ),
            80.0,
            "male",
        )
        assertEquals(listOf("Barbell Back Squat", "Barbell Bench Press"), ranks.map { it.name })
        assertEquals(Rank.B, ranks[1].rank)
        assertEquals(Rank.S, ranks[0].rank)
    }
}

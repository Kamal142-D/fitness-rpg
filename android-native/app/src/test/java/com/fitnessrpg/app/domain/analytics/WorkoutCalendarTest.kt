package com.fitnessrpg.app.domain.analytics

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCalendarTest {
    @Test
    fun `completed dates are local distinct and ignore invalid timestamps`() {
        val sessions = listOf(
            session("1", "2026-08-28T22:30:00Z"),
            session("2", "2026-08-29T08:00:00+03:00"),
            session("3", "not-a-date"),
            session("4", null),
        )

        assertEquals(
            setOf(LocalDate.of(2026, 8, 29)),
            completedWorkoutDates(sessions, ZoneId.of("Asia/Riyadh")),
        )
    }

    @Test
    fun `weekly count uses monday through sunday and distinct days`() {
        val dates = setOf(
            LocalDate.of(2026, 8, 23), // Sunday before this week
            LocalDate.of(2026, 8, 24), // Monday
            LocalDate.of(2026, 8, 28), // Friday / today
            LocalDate.of(2026, 8, 30), // Sunday
            LocalDate.of(2026, 8, 31), // Next Monday
        )

        assertEquals(2, workoutsThisWeek(dates, LocalDate.of(2026, 8, 28)))
    }

    @Test
    fun `weekly goal defaults and stays within calendar limits`() {
        assertEquals(3, normalizedWeeklyGoal(null))
        assertEquals(1, normalizedWeeklyGoal(0))
        assertEquals(5, normalizedWeeklyGoal(5))
        assertEquals(7, normalizedWeeklyGoal(9))
    }

    private fun session(id: String, completedAt: String?) = SessionSummary(
        id = id,
        name = null,
        completedAt = completedAt,
        gateClearRank = null,
        totalVolumeKg = null,
        durationSeconds = null,
    )
}

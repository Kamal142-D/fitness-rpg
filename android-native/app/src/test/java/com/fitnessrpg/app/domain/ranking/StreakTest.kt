package com.fitnessrpg.app.domain.ranking

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    @Test
    fun `increments on training and tracks the longest`() {
        var s = StreakState(0, 0)
        s = updateStreak(s, DayOutcome(didTrain = true, isScheduledRest = false))
        s = updateStreak(s, DayOutcome(didTrain = true, isScheduledRest = false))
        assertEquals(StreakState(2, 2), s)
    }

    @Test
    fun `preserves the streak on a scheduled rest day`() {
        val s = updateStreak(StreakState(3, 5), DayOutcome(didTrain = false, isScheduledRest = true))
        assertEquals(StreakState(3, 5), s)
    }

    @Test
    fun `resets on a missed training day but keeps the longest`() {
        val s = updateStreak(StreakState(4, 6), DayOutcome(didTrain = false, isScheduledRest = false))
        assertEquals(StreakState(0, 6), s)
    }

    @Test
    fun `adherenceRatio is completed over planned, clamped to 0-1`() {
        assertEquals(0.75, adherenceRatio(3, 4), 1e-9)
        assertEquals(1.0, adherenceRatio(5, 4), 1e-9)
        assertEquals(0.0, adherenceRatio(0, 0), 1e-9)
    }

    @Test
    fun `disciplineScore weights adherence with a capped streak bonus`() {
        assertEquals(75.0, disciplineScore(DisciplineInput(0, 1.0)), 1e-9)
        assertEquals(100.0, disciplineScore(DisciplineInput(10, 1.0)), 1e-9)
        assertEquals(47.5, disciplineScore(DisciplineInput(4, 0.5)), 1e-4)
    }

    @Test
    fun `disciplineScore caps the streak contribution`() {
        assertEquals(25.0, disciplineScore(DisciplineInput(999, 0.0)), 1e-9)
    }
}

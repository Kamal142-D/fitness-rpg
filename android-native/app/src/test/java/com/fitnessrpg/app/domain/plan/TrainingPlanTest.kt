package com.fitnessrpg.app.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainingPlanTest {

    private val today = 1000L

    @Test
    fun `default plan is push pull legs rest upper lower rest`() {
        val p = defaultPlan(today)
        assertEquals(listOf("Push", "Pull", "Legs & Abs", "Rest", "Full Upper", "Full Lower", "Rest"), p.slots.map { it.label })
        assertEquals("Push", p.current?.label)
        assertEquals("Pull", p.next?.label)
    }

    @Test
    fun `a fresh workout day is TODAY_WORKOUT`() {
        val s = defaultPlan(today).status(today)!!
        assertEquals(PlanState.TODAY_WORKOUT, s.state)
        assertEquals("Push", s.current.label)
    }

    @Test
    fun `a workout not done for a day becomes MISSED_WORKOUT`() {
        val s = defaultPlan(today).status(today + 1)!!
        assertEquals(PlanState.MISSED_WORKOUT, s.state)
        assertEquals(1L, s.daysOverdue)
        assertEquals("Push", s.current.label)
    }

    @Test
    fun `advancing moves to the next slot and resets the clock`() {
        val p = defaultPlan(today).advanced(today + 1)
        assertEquals("Pull", p.current?.label)
        assertEquals(TODAY_CLOCK, p.startedEpochDay)
    }

    @Test
    fun `skip to next from a missed workout lands on the following slot`() {
        val missed = defaultPlan(today) // on Push
        val skipped = missed.advanced(today + 2)
        assertEquals("Pull", skipped.current?.label)
        assertEquals(PlanState.TODAY_WORKOUT, skipped.status(today + 2)!!.state)
    }

    @Test
    fun `do it now renews the current workout for today`() {
        val renewed = defaultPlan(today).renewedToday(today + 3)
        assertEquals("Push", renewed.current?.label)
        assertEquals(PlanState.TODAY_WORKOUT, renewed.status(today + 3)!!.state)
    }

    @Test
    fun `a rest slot shows TODAY_REST`() {
        val onRest = TrainingPlan(DEFAULT_PLAN_SLOTS, currentIndex = 3, startedEpochDay = today)
        assertEquals(PlanState.TODAY_REST, onRest.status(today)!!.state)
    }

    @Test
    fun `a rest day rolls forward automatically after a day passes`() {
        val onRest = TrainingPlan(DEFAULT_PLAN_SLOTS, currentIndex = 3, startedEpochDay = today)
        val rolled = onRest.normalized(today + 1)
        assertEquals("Full Upper", rolled.current?.label)
    }

    @Test
    fun `normalize does not roll a rest that is still today`() {
        val onRest = TrainingPlan(DEFAULT_PLAN_SLOTS, currentIndex = 3, startedEpochDay = today)
        assertEquals(3, onRest.normalized(today).currentIndex)
    }

    @Test
    fun `the cycle wraps back to the start`() {
        var p = TrainingPlan(DEFAULT_PLAN_SLOTS, currentIndex = 6, startedEpochDay = today)
        p = p.advanced(today + 1)
        assertEquals("Push", p.current?.label)
        assertEquals(0, p.currentIndex)
    }

    @Test
    fun `an empty plan has no status`() {
        assertNull(TrainingPlan(emptyList()).status(today))
    }

    companion object {
        private const val TODAY_CLOCK = 1001L
    }
}

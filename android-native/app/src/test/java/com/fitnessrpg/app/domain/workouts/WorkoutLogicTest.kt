package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.TemplateExercise
import com.fitnessrpg.app.domain.model.TemplateExerciseWithExercise
import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogicTest {

    private fun mkEx(exId: String, name: String, sets: Int, rest: Int) = TemplateExerciseWithExercise(
        templateExercise = TemplateExercise("te_$exId", "tpl1", exId, 0, sets, 5, 8, 8.0, rest),
        exercise = Exercise(exId, name, "chest", "chest", emptyList(), "barbell", "strength", true, ""),
    )

    private fun detail() = GateDetail(
        template = GateTemplate("tpl1", null, "Push", "Chest, shoulders", 50, "C", true, "", ""),
        exercises = listOf(mkEx("ex1", "Bench", 2, 120), mkEx("ex2", "OHP", 2, 90)),
    )

    @Test
    fun `builds exercises and target-count sets with smart rep defaults`() {
        val w = createActiveWorkout(detail(), 0L)
        assertEquals(2, w.exercises.size)
        assertEquals(2, w.exercises[0].sets.size)
        assertEquals(5, w.exercises[0].sets[0].reps)
        assertNull(w.exercises[0].sets[0].weightKg)
        assertEquals(null, w.gateDifficulty)
        assertTrue(Regex("[0-9a-f-]{36}").containsMatchIn(w.sessionId))
    }

    @Test
    fun `completeSet stamps the set, starts rest, and pre-fills the next set`() {
        var w = createActiveWorkout(detail(), 0L)
        w = updateSet(w, 0, 0, SetPatch(weightKg = 100.0, reps = 5))
        w = completeSet(w, 0, 0, 10_000L)
        assertTrue(w.exercises[0].sets[0].isCompleted)
        assertEquals(10_000L + 120 * 1000L, w.restEndsAt)
        assertEquals(100.0, w.exercises[0].sets[1].weightKg!!, 1e-9)
    }

    @Test
    fun `updateSet does not mutate the input state`() {
        val w = createActiveWorkout(detail(), 0L)
        val next = updateSet(w, 0, 0, SetPatch(weightKg = 80.0))
        assertNull(w.exercises[0].sets[0].weightKg)
        assertEquals(80.0, next.exercises[0].sets[0].weightKg!!, 1e-9)
    }

    @Test
    fun `addSet appends copying the last set and removeSet renumbers`() {
        var w = createActiveWorkout(detail(), 0L)
        w = updateSet(w, 0, 1, SetPatch(weightKg = 90.0, reps = 6))
        w = addSet(w, 0)
        assertEquals(3, w.exercises[0].sets.size)
        assertEquals(90.0, w.exercises[0].sets[2].weightKg!!, 1e-9)
        w = removeSet(w, 0, 1)
        assertEquals(2, w.exercises[0].sets.size)
        assertEquals(listOf(1, 2), w.exercises[0].sets.map { it.setNumber })
    }

    @Test
    fun `will not remove the last remaining set`() {
        var w = createActiveWorkout(detail(), 0L)
        w = removeSet(w, 0, 0)
        w = removeSet(w, 0, 0)
        assertTrue(w.exercises[0].sets.size >= 1)
    }

    @Test
    fun `toggles warm-up and clamps exercise navigation`() {
        var w = createActiveWorkout(detail(), 0L)
        w = toggleWarmup(w, 0, 0)
        assertTrue(w.exercises[0].sets[0].isWarmup)
        w = setCurrentExercise(w, 99)
        assertEquals(1, w.currentExerciseIndex)
        w = setCurrentExercise(w, -5)
        assertEquals(0, w.currentExerciseIndex)
    }

    @Test
    fun `counts completed working sets only`() {
        var w = createActiveWorkout(detail(), 0L)
        w = completeSet(w, 0, 0, 1000L)
        w = toggleWarmup(w, 0, 1)
        w = completeSet(w, 0, 1, 2000L)
        assertEquals(1, completedWorkingSetCount(w))
    }
}

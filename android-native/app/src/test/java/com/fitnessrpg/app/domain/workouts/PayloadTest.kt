package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.TemplateExercise
import com.fitnessrpg.app.domain.model.TemplateExerciseWithExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayloadTest {

    private fun mkEx(exId: String, name: String) = TemplateExerciseWithExercise(
        templateExercise = TemplateExercise("te_$exId", "tpl1", exId, 0, 2, 5, 8, 8.0, 90),
        exercise = Exercise(exId, name, "chest", "chest", emptyList(), "barbell", "strength", true, ""),
    )

    private fun detail() = GateDetail(
        template = GateTemplate("tpl1", null, "Push", "Chest", 50, "C", true, "", ""),
        exercises = listOf(mkEx("ex1", "Bench"), mkEx("ex2", "OHP")),
    )

    @Test
    fun `persists only completed sets, only exercises with any, and computes aggregates`() {
        var w = createActiveWorkout(detail(), 0L) // startedAt = epoch 0
        w = updateSet(w, 0, 0, SetPatch(weightKg = 60.0, reps = 5))
        w = toggleWarmup(w, 0, 0)
        w = completeSet(w, 0, 0, 1000L)
        w = updateSet(w, 0, 1, SetPatch(weightKg = 100.0, reps = 5))
        w = completeSet(w, 0, 1, 2000L)
        // ex2: nothing completed

        val (payload, aggregates) = buildCompletionPayload(w, 60_000L)

        assertEquals(1, payload.exercises.size) // ex2 omitted
        assertEquals(2, payload.exercises[0].sets.size)
        assertEquals(listOf(1, 2), payload.exercises[0].sets.map { it.setNumber })

        assertEquals(500.0, aggregates.totalVolumeKg, 1e-9) // 100 * 5
        assertEquals(1, aggregates.completedSets)
        assertEquals(1, aggregates.exerciseCount)
        assertEquals(60, aggregates.durationSeconds)

        val working = payload.exercises[0].sets.first { !it.isWarmup }
        val warm = payload.exercises[0].sets.first { it.isWarmup }
        assertEquals(116.67, working.estimated1rmKg!!, 1e-9)
        assertNull(warm.estimated1rmKg)

        assertEquals(w.sessionId, payload.session.id)
        assertEquals(500.0, payload.session.totalVolumeKg, 1e-9)
    }

    @Test
    fun `produces no exercises when nothing was completed`() {
        val w = createActiveWorkout(detail(), 0L)
        val (payload, aggregates) = buildCompletionPayload(w, 1000L)
        assertEquals(0, payload.exercises.size)
        assertEquals(0, aggregates.completedSets)
    }
}

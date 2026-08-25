package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchTest {
    private val catalog = listOf(
        exercise("1", "Barbell Bench Press", "chest", "barbell", listOf("flat bench")),
        exercise("2", "Cable Rear Delt Fly", "rear delts", "cable"),
        exercise("3", "Dumbbell Curl", "biceps", "dumbbell"),
    )

    @Test fun `searches full and partial names`() {
        assertEquals(listOf("1"), searchExercises(catalog, "barbell bench").map { it.id })
        assertEquals(listOf("1"), searchExercises(catalog, "bench").map { it.id })
    }
    @Test fun `searches muscles equipment and aliases`() {
        assertEquals("2", searchExercises(catalog, "rear delt").single().id)
        assertEquals("2", searchExercises(catalog, "cable").single().id)
        assertEquals("1", searchExercises(catalog, "flat bench").single().id)
    }
    @Test fun `filters and returns empty results`() {
        assertEquals("3", searchExercises(catalog, "", ExerciseFilters(muscle = "biceps", equipment = "dumbbell")).single().id)
        assertTrue(searchExercises(catalog, "no-such-exercise").isEmpty())
    }

    private fun exercise(id: String, name: String, muscle: String, equipment: String, aliases: List<String> = emptyList()) =
        Exercise(id, name, "strength", muscle, emptyList(), equipment, "strength", true, aliases = aliases)
}

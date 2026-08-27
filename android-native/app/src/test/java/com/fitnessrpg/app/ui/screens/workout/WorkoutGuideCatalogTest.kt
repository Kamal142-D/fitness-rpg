package com.fitnessrpg.app.ui.screens.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutGuideCatalogTest {
    private val catalog = listOf(
        WorkoutGuideEntry("Bench Press", "bench-press", "Barbell"),
        WorkoutGuideEntry("Incline Dumbbell Press", "incline-dumbbell-press", "Dumbbell"),
        WorkoutGuideEntry("Rear Delt Fly", "rear-delt-fly", "Dumbbell"),
    )

    @Test
    fun `matches an exact exercise name`() {
        val match = findWorkoutGuideEntry("BENCH PRESS", "barbell", catalog)

        assertEquals("bench-press", match?.slug)
    }

    @Test
    fun `matches a catalog name contained in a qualified source name`() {
        val match = findWorkoutGuideEntry("barbell bench press", "barbell", catalog)

        assertEquals("bench-press", match?.slug)
    }

    @Test
    fun `uses equipment to avoid a visually incorrect match`() {
        val match = findWorkoutGuideEntry("dumbbell bench press", "dumbbell", catalog)

        assertNull(match)
    }

    @Test
    fun `does not guess from a single shared word`() {
        val match = findWorkoutGuideEntry("cable reverse fly", "cable", catalog)

        assertNull(match)
    }
}

package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GateMappersTest {

    private fun template(
        estimatedDurationMinutes: Int? = 50,
        difficulty: String? = "C",
        description: String? = "Chest, shoulders, triceps",
    ) = GateTemplate(
        id = "t1", userId = null, name = "Push", description = description,
        estimatedDurationMinutes = estimatedDurationMinutes, difficulty = difficulty,
        isSystemTemplate = true, createdAt = "", updatedAt = "",
    )

    @Test
    fun `maps every rank to an intensity label`() {
        assertEquals("Light", intensityForDifficulty(Rank.E))
        assertEquals("Light", intensityForDifficulty(Rank.D))
        assertEquals("Moderate", intensityForDifficulty(Rank.C))
        assertEquals("Hard", intensityForDifficulty(Rank.B))
        assertEquals("Brutal", intensityForDifficulty(Rank.A))
        assertEquals("Brutal", intensityForDifficulty(Rank.S))
    }

    @Test
    fun `templateDifficulty passes valid ranks and defaults invalid to D`() {
        assertEquals(Rank.B, templateDifficulty("B"))
        assertEquals(Rank.D, templateDifficulty(null as String?))
        assertEquals(Rank.D, templateDifficulty("Z"))
    }

    @Test
    fun `muscleGroupsFor splits a description into groups`() {
        assertEquals(
            listOf("Chest", "shoulders", "triceps"),
            muscleGroupsFor("Chest, shoulders, triceps"),
        )
        assertEquals(emptyList<String>(), muscleGroupsFor(null))
    }

    @Test
    fun `new template is not assessed instead of using planned difficulty`() {
        val g = templateToSuggestedGate(template())
        assertEquals("Push", g.name)
        assertEquals(null, g.difficulty)
        assertEquals(50, g.durationMinutes)
        assertEquals("Not Assessed", g.intensity)
        assertTrue(g.muscleGroups.contains("Chest"))
    }

    @Test
    fun `template uses last assessed difficulty`() {
        val g = templateToSuggestedGate(template().copy(lastDifficultyRank = "B"))
        assertEquals(Rank.B, g.difficulty)
        assertEquals("Hard", g.intensity)
    }

    @Test
    fun `templateToSuggestedGate falls back to a default duration`() {
        assertEquals(45, templateToSuggestedGate(template(estimatedDurationMinutes = null)).durationMinutes)
    }

    @Test
    fun `formats rep ranges`() {
        assertEquals("5-8", formatRepRange(5, 8))
        assertEquals("8", formatRepRange(8, 8))
        assertEquals("5+", formatRepRange(5, null))
        assertEquals("—", formatRepRange(null, null))
    }

    @Test
    fun `formats sets and reps, and time-based`() {
        assertEquals("4 × 5-8", formatTargets(4, 5, 8))
        assertEquals("3 sets", formatTargets(3, null, null))
    }
}

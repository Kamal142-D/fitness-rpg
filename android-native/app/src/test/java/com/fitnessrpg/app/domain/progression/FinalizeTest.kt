package com.fitnessrpg.app.domain.progression

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Test

class FinalizeTest {
    @Test
    fun `applies XP with level-up and gates Hunter Rank on the pillars (no discipline)`() {
        val out = buildProgressionUpdate(
            ProgressionUpdateInput(
                current = ProgressionSnapshot(1, 0, 0),
                currentAttributes = CurrentAttributes(0.0, 0.0, 0.0, 0.0),
                xpEarned = 300,
                streak = StreakSnapshot(3, 5),
                attributes = AttributeInputs(strength = 70.0, physique = null, endurance = null, discipline = 80.0),
            ),
        )
        assertEquals(2, out.level)
        assertEquals(200, out.currentXp)
        assertEquals(300, out.lifetimeXp)
        assertEquals(70.0, out.strengthScore, 1e-9)
        assertEquals(80.0, out.disciplineScore, 1e-9)
        assertEquals(0.0, out.physiqueScore, 1e-9)
        // Only strength is known + conditioning unknown -> provisional, capped at C.
        // Discipline (80) does NOT lift the Hunter Rank.
        assertEquals(70.0, out.hunterScore, 1e-9)
        assertEquals(Rank.C, out.hunterRank)
        assertEquals(3, out.currentStreakDays)
    }

    @Test
    fun `stays at E when no physical pillars are known`() {
        val out = buildProgressionUpdate(
            ProgressionUpdateInput(
                current = ProgressionSnapshot(5, 10, 999),
                currentAttributes = CurrentAttributes(0.0, 0.0, 0.0, 0.0),
                xpEarned = 0,
                streak = StreakSnapshot(0, 2),
                attributes = AttributeInputs(null, null, null, null),
            ),
        )
        assertEquals(5, out.level)
        assertEquals(0.0, out.hunterScore, 1e-9)
        assertEquals(Rank.E, out.hunterRank)
    }
}

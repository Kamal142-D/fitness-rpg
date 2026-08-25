package com.fitnessrpg.app.domain.progression

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardsTest {
    @Test
    fun `sums base, per-set, PR, and gate-clear bonuses`() {
        // 300 + 8*10 + 1*50 + 200 (B bonus) = 630
        assertEquals(630, xpForWorkout(WorkoutXpInput(true, 8, 1, Rank.B)))
    }

    @Test
    fun `awards nothing for an incomplete, empty workout`() {
        assertEquals(0, xpForWorkout(WorkoutXpInput(false, 0, 0, null)))
    }

    @Test
    fun `uses the S gate-clear bonus of 300`() {
        assertEquals(600, xpForWorkout(WorkoutXpInput(true, 0, 0, Rank.S)))
    }

    @Test
    fun `levels up once when XP crosses the requirement`() {
        val r = applyXp(ProgressionSnapshot(1, 0, 0), 300)
        assertEquals(2, r.level)
        assertEquals(200, r.currentXp)
        assertEquals(300, r.lifetimeXp)
        assertTrue(r.leveledUp)
        assertEquals(1, r.levelsGained)
    }

    @Test
    fun `rolls over multiple levels for a large gain`() {
        val r = applyXp(ProgressionSnapshot(1, 0, 0), 100_000)
        assertTrue(r.levelsGained > 1)
        assertEquals(100_000, r.lifetimeXp)
    }

    @Test
    fun `stays in-level for a small gain`() {
        val r = applyXp(ProgressionSnapshot(1, 0, 0), 50)
        assertEquals(1, r.level)
        assertEquals(50, r.currentXp)
        assertFalse(r.leveledUp)
        assertEquals(0, r.levelsGained)
    }

    @Test
    fun `is a no-op for zero XP`() {
        val r = applyXp(ProgressionSnapshot(3, 40, 900), 0)
        assertEquals(3, r.level)
        assertEquals(40, r.currentXp)
        assertFalse(r.leveledUp)
    }
}

package com.fitnessrpg.app.ui.components

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Test

class AttributeDisplayTest {
    @Test
    fun `authoritative capped pillar rank wins over raw score rank`() {
        val physique = attributeDisplay(score = 70.0, authoritativeRank = Rank.C, authoritativeRp = 99)

        assertEquals(Rank.C, physique.rank)
        assertEquals(99, physique.rp)
    }

    @Test
    fun `profile values match ranking values from screenshot regression`() {
        val strength = attributeDisplay(score = 28.0, authoritativeRank = Rank.C, authoritativeRp = 13)
        val conditioning = attributeDisplay(score = 36.0, authoritativeRank = Rank.C, authoritativeRp = 42)

        assertEquals(AttributeDisplay(Rank.C, 13), strength)
        assertEquals(AttributeDisplay(Rank.C, 42), conditioning)
    }

    @Test
    fun `rp is constrained to valid display progress`() {
        assertEquals(100, attributeDisplay(70.0, Rank.C, 140).rp)
        assertEquals(0, attributeDisplay(28.0, Rank.C, -4).rp)
    }
}

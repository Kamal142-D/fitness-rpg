package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributesTest {

    @Test
    fun `strengthScore averages ranked exercise scores, or null when none`() {
        assertNull(strengthScore(emptyList()))
        assertEquals(60.0, strengthScore(listOf(40.0, 60.0, 80.0))!!, 1e-9)
    }

    @Test
    fun `physiqueScore is null without an assessment`() {
        assertNull(physiqueScore(null))
    }

    @Test
    fun `physiqueScore peaks a healthy body fat and blends with muscle development`() {
        // bodyFat 12% (male) -> 100; SMM 36/80 = 45% -> ~91.7; mean -> ~95.8
        val score = physiqueScore(PhysiqueInput(12.0, 36.0, 80.0, "male"))
        assertEquals(95.8, score!!, 0.05)
    }

    @Test
    fun `physiqueScore does not reward ever-lower body fat`() {
        val healthy = physiqueScore(PhysiqueInput(12.0, null, null, "male"))!!
        val veryLow = physiqueScore(PhysiqueInput(4.0, null, null, "male"))!!
        assertTrue(veryLow < healthy)
    }

    @Test
    fun `enduranceScore maps weekly training minutes, null when no data`() {
        assertNull(enduranceScore(null))
        assertEquals(0.0, enduranceScore(0.0)!!, 1e-9)
        assertEquals(70.0, enduranceScore(150.0)!!, 1e-9)
    }

    @Test
    fun `hunterScore applies attribute weights`() {
        // 80*.4 + 60*.3 + 40*.15 + 60*.15 = 65
        val score = hunterScore(HunterAttributes(80.0, 60.0, 40.0, 60.0))
        assertEquals(65.0, score, 1e-4)
        assertEquals(Rank.A, hunterRank(score))
    }

    @Test
    fun `hunterScore renormalizes over available attributes`() {
        assertEquals(80.0, hunterScore(HunterAttributes(80.0, null, null, null)), 1e-9)
    }

    @Test
    fun `hunterScore is neutral when nothing is known`() {
        assertEquals(60.0, hunterScore(HunterAttributes(null, null, null, null)), 1e-9)
    }

    @Test
    fun `limitingAttribute returns the lowest available attribute`() {
        assertEquals(
            AttributeName.ENDURANCE,
            limitingAttribute(HunterAttributes(80.0, 60.0, 40.0, 60.0)),
        )
        assertEquals(
            AttributeName.DISCIPLINE,
            limitingAttribute(HunterAttributes(80.0, null, null, 70.0)),
        )
        assertNull(limitingAttribute(HunterAttributes(null, null, null, null)))
    }
}

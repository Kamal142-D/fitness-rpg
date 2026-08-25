package com.fitnessrpg.app.domain.ranking

import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GateScoreTest {

    @Test
    fun `applies the documented weights`() {
        // 80*.5 + 100*.2 + 60*.15 + 50*.1 + 60*.05 = 77
        val score = computeGateScore(
            GateScoreInput(performance = 80.0, completion = 100.0, progress = 60.0, pr = 50.0, quality = 60.0),
        )
        assertEquals(77.0, score, 1e-4)
        assertEquals(Rank.A, gateClearRank(score))
    }

    @Test
    fun `renormalizes when a factor is missing instead of scoring zero`() {
        // Drop progress (weight .15): (80*.5 + 100*.2 + 50*.1 + 60*.05) / .85 = 80
        val score = computeGateScore(
            GateScoreInput(performance = 80.0, completion = 100.0, progress = null, pr = 50.0, quality = 60.0),
        )
        assertEquals(80.0, score, 1e-4)
    }

    @Test
    fun `clamps out-of-range component values`() {
        val score = computeGateScore(
            GateScoreInput(500.0, 500.0, 500.0, 500.0, 500.0),
        )
        assertEquals(100.0, score, 1e-9)
    }

    @Test
    fun `returns the neutral score when every factor is missing`() {
        assertEquals(60.0, weightedRenormalized(listOf(WeightedComponent(null, 1.0))), 1e-9)
    }

    @Test
    fun `completionScore is completed over planned percent, clamped`() {
        assertEquals(60.0, completionScore(3, 5), 1e-9)
        assertEquals(100.0, completionScore(10, 5), 1e-9)
        assertEquals(100.0, completionScore(2, 0), 1e-9)
        assertEquals(0.0, completionScore(0, 0), 1e-9)
    }

    @Test
    fun `progressScore is null without history and a ratio otherwise`() {
        assertNull(progressScore(1000.0, null))
        assertEquals(88.0, progressScore(1100.0, 1000.0)!!, 1e-4) // ratio 1.1
    }

    @Test
    fun `prComponentScore rewards PRs without punishing their absence`() {
        assertEquals(50.0, prComponentScore(0), 1e-9)
        assertEquals(72.0, prComponentScore(1), 1e-9)
        assertEquals(86.0, prComponentScore(2), 1e-9)
        assertEquals(100.0, prComponentScore(5), 1e-9)
    }

    @Test
    fun `qualityScore reflects valid-RPE fraction, neutral when none logged`() {
        assertEquals(60.0, qualityScore(emptyList()), 1e-9)
        assertEquals(60.0, qualityScore(listOf<Double?>(null, null)), 1e-9)
        assertEquals(100.0, qualityScore(listOf<Double?>(8.0, 9.0)), 1e-9)
        assertEquals(50.0, qualityScore(listOf<Double?>(8.0, 11.0)), 1e-9)
    }
}

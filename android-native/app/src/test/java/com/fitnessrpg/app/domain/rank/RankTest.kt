package com.fitnessrpg.app.domain.rank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankTest {

    @Test
    fun `clampScore passes through in-range values`() {
        assertEquals(0.0, clampScore(0.0), 0.0)
        assertEquals(50.0, clampScore(50.0), 0.0)
        assertEquals(100.0, clampScore(100.0), 0.0)
    }

    @Test
    fun `clampScore clamps below 0 and above 100`() {
        assertEquals(0.0, clampScore(-25.0), 0.0)
        assertEquals(100.0, clampScore(999.0), 0.0)
    }

    @Test
    fun `clampScore treats NaN as 0`() {
        assertEquals(0.0, clampScore(Double.NaN), 0.0)
    }

    @Test
    fun `scoreToRank maps the documented band midpoints`() {
        assertEquals(Rank.E, scoreToRank(10.0))
        assertEquals(Rank.D, scoreToRank(27.0))
        assertEquals(Rank.C, scoreToRank(42.0))
        assertEquals(Rank.B, scoreToRank(57.0))
        assertEquals(Rank.A, scoreToRank(72.0))
        assertEquals(Rank.S, scoreToRank(90.0))
    }

    @Test
    fun `scoreToRank is correct at every band boundary inclusive`() {
        for (band in RANK_THRESHOLDS) {
            assertEquals(band.rank, scoreToRank(band.min.toDouble()))
            assertEquals(band.rank, scoreToRank(band.max.toDouble()))
        }
    }

    @Test
    fun `scoreToRank clamps out-of-range scores to the end ranks`() {
        assertEquals(Rank.E, scoreToRank(-50.0))
        assertEquals(Rank.S, scoreToRank(150.0))
    }

    @Test
    fun `scoreToRank never throws and always returns a known rank`() {
        val samples = listOf(Double.NaN, -1.0, 0.0, 19.5, 34.9, 65.0, 79.999, 80.0, 100.0, 100.1)
        for (s in samples) {
            assertTrue(scoreToRank(s) in Rank.entries)
        }
    }

    @Test
    fun `rank thresholds cover 0-100 contiguously with no gaps`() {
        assertEquals(0, RANK_THRESHOLDS.first().min)
        assertEquals(100, RANK_THRESHOLDS.last().max)
        for (i in 0 until RANK_THRESHOLDS.size - 1) {
            assertEquals(RANK_THRESHOLDS[i].max + 1, RANK_THRESHOLDS[i + 1].min)
        }
    }

    @Test
    fun `rank thresholds are weakest-to-strongest matching Rank order`() {
        assertEquals(Rank.entries.toList(), RANK_THRESHOLDS.map { it.rank })
    }
}

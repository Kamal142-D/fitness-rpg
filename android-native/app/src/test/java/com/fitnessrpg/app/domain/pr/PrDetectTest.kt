package com.fitnessrpg.app.domain.pr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrDetectTest {

    private fun set(n: Int, w: Double?, reps: Int?, e: Double?, warm: Boolean = false) =
        DetectSet(n, w, reps, e, warm)

    private fun ex(sets: List<DetectSet>, id: String = "ex1", order: Int = 0) =
        DetectExercise(id, order, sets)

    @Test
    fun `treats a first-ever attempt as a baseline, but records stats`() {
        val (prs, stats) = detectPRs(listOf(ex(listOf(set(1, 100.0, 5, 116.67)))), emptyMap())
        assertEquals(0, prs.size)
        assertEquals(100.0, stats[0].bestWeightKg!!, 1e-9)
        assertEquals(5.0, stats[0].bestReps!!, 1e-9)
        assertEquals(116.67, stats[0].bestEstimated1rmKg!!, 1e-9)
        assertEquals(500.0, stats[0].bestVolumeKg!!, 1e-9)
    }

    @Test
    fun `flags a weight PR when it beats the prior best`() {
        val prior = mapOf("ex1" to PriorStat(100.0, 8.0, 130.0, 1000.0))
        val (prs, _) = detectPRs(listOf(ex(listOf(set(1, 110.0, 5, 128.0)))), prior)
        val weightPR = prs.first { it.recordType == RecordType.WEIGHT }
        assertEquals(100.0, weightPR.previousValue!!, 1e-9)
        assertEquals(110.0, weightPR.newValue, 1e-9)
        assertEquals(1, weightPR.setNumber)
        assertEquals(listOf(RecordType.WEIGHT), prs.map { it.recordType })
    }

    @Test
    fun `does not flag a PR when equal to or below the prior best`() {
        val prior = mapOf("ex1" to PriorStat(100.0, 5.0, 116.67, 500.0))
        val (prs, _) = detectPRs(listOf(ex(listOf(set(1, 100.0, 5, 116.67)))), prior)
        assertEquals(0, prs.size)
    }

    @Test
    fun `emits at most one PR per type per exercise (the best set)`() {
        val prior = mapOf("ex1" to PriorStat(90.0, 4.0, 100.0, 400.0))
        val (prs, _) = detectPRs(listOf(ex(listOf(set(1, 95.0, 6, 105.0), set(2, 100.0, 8, 120.0)))), prior)
        val weightPRs = prs.filter { it.recordType == RecordType.WEIGHT }
        assertEquals(1, weightPRs.size)
        assertEquals(100.0, weightPRs[0].newValue, 1e-9)
        assertEquals(2, weightPRs[0].setNumber)
    }

    @Test
    fun `ignores warm-up sets entirely`() {
        val prior = mapOf("ex1" to PriorStat(100.0, 5.0, 116.0, 500.0))
        val (prs, _) = detectPRs(listOf(ex(listOf(set(1, 200.0, 1, 200.0, true), set(2, 90.0, 5, 105.0)))), prior)
        assertNull(prs.firstOrNull { it.recordType == RecordType.WEIGHT })
    }

    @Test
    fun `prioritizes estimated_1rm first, then weight, then volume`() {
        val prs = listOf(
            DetectedPR("a", 0, 1, RecordType.VOLUME, 100.0, 150.0),
            DetectedPR("a", 0, 1, RecordType.ESTIMATED_1RM, 100.0, 110.0),
            DetectedPR("a", 0, 1, RecordType.WEIGHT, 100.0, 105.0),
        )
        assertEquals(
            listOf(RecordType.ESTIMATED_1RM, RecordType.WEIGHT, RecordType.VOLUME),
            prioritizePRs(prs).map { it.recordType },
        )
        assertEquals(2, prioritizePRs(prs, 2).size)
    }
}

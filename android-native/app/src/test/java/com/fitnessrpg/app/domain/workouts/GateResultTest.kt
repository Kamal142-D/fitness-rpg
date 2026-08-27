package com.fitnessrpg.app.domain.workouts

import com.fitnessrpg.app.domain.pr.PriorStat
import com.fitnessrpg.app.domain.pr.RecordType
import com.fitnessrpg.app.domain.rank.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GateResultTest {

    private fun workingSet(n: Int) =
        CompletionSetPayload(n, 100.0, 5, null, false, true, 116.67, null)

    private fun payload() = CompletionPayload(
        session = CompletionSession(
            "s", "t", "Push", "C", "", "", 0, 1000.0,
            null, null, null, null, null, null,
        ),
        exercises = listOf(
            CompletionExercisePayload("ex1", 0, null, null, null, listOf(workingSet(1), workingSet(2))),
        ),
    )

    private val aggregates = CompletionAggregates("Push", "C", 0, 1000.0, 2, 2, 1)

    @Test
    fun `first gate is baseline and cannot receive an elite clear grade`() {
        val r = computeGateResult(payload(), emptyMap(), aggregates, emptyList())
        assertEquals(100.0, r.completionScore, 1e-9)
        assertTrue(r.gateClearRank.ordinal <= Rank.B.ordinal)
        assertEquals(com.fitnessrpg.app.domain.rankings.ExerciseRankingMode.UNRANKED, r.perExercise[0].rankingMode)
        assertEquals("Baseline", r.perExercise[0].todayLabel)
        assertEquals(null, r.perExercise[0].performanceGrade)
        assertTrue(r.clearProvisional)
    }

    @Test
    fun `rewards meaningful PRs in the XP total`() {
        val r = computeGateResult(payload(), emptyMap(), aggregates, listOf(RecordType.ESTIMATED_1RM))
        val noPr = computeGateResult(payload(), emptyMap(), aggregates, emptyList())
        assertTrue(r.xpEarned > noPr.xpEarned)
    }
}

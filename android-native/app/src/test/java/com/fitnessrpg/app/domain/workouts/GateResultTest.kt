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
    fun `computes gate score, clear rank, grades and XP with no history`() {
        val r = computeGateResult(payload(), emptyMap(), aggregates, emptyList())
        assertEquals(100.0, r.completionScore, 1e-9)
        // V2: target 30%, completion 35%, progress 25%, PR 10% (missing progress renormalized).
        assertEquals(77.33, r.gateScore, 0.05)
        assertEquals(Rank.A, r.gateClearRank)
        assertEquals(Rank.B, r.perExercise[0].performanceGrade) // neutral 60 -> B
        // 300 base + 2*10 sets + 0 PRs + 250 (A bonus) = 570
        assertEquals(570, r.xpEarned)
    }

    @Test
    fun `rewards meaningful PRs in the XP total`() {
        val r = computeGateResult(payload(), emptyMap(), aggregates, listOf(RecordType.ESTIMATED_1RM))
        assertTrue(r.xpEarned > 570)
    }
}

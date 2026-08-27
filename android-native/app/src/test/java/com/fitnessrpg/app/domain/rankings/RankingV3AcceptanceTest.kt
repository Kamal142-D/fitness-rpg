package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.ranking.calculatePersonalExerciseTier
import com.fitnessrpg.app.domain.workouts.CompletionAggregates
import com.fitnessrpg.app.domain.workouts.CompletionExercisePayload
import com.fitnessrpg.app.domain.workouts.CompletionPayload
import com.fitnessrpg.app.domain.workouts.CompletionSession
import com.fitnessrpg.app.domain.workouts.CompletionSetPayload
import com.fitnessrpg.app.domain.workouts.computeGateResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingV3AcceptanceTest {

    @Test
    fun `current user regression remains safely provisional`() {
        val physique = computePhysiqueRank(
            BodyCompositionData(
                weightKg = 71.5,
                heightCm = 171.0,
                bodyFatPercent = 18.0,
                muscleMassKg = 55.2,
                waistCm = null,
                sex = "male",
            ),
        )
        val strength = computeStrengthRank(emptyList(), 71.5, "male")
        val conditioning = computeConditioningRank(null)
        val hunter = computeHunterRank(physique, strength, conditioning)

        assertEquals(Rank.B, physique.rankCap)
        assertTrue(physique.rank!!.ordinal <= Rank.B.ordinal)
        assertNull(strength.score)
        assertTrue(strength.provisional)
        assertNull(conditioning.score)
        assertTrue(hunter.provisional)
        assertTrue(hunter.rank.ordinal <= Rank.C.ordinal)
        assertEquals(Rank.C, hunter.rankCap)
    }

    @Test
    fun `current pull day does not turn completion into S exercise grades`() {
        val exercises = listOf("cable-row", "lat-pulldown", "preacher-curl")
        val payload = CompletionPayload(
            session = CompletionSession(
                id = "session", templateId = "pull", name = "Pull Day", gateDifficulty = null,
                startedAt = "", completedAt = "", durationSeconds = 3600, totalVolumeKg = 3000.0,
                completionScore = null, progressScore = null, qualityScore = null, gateScore = null,
                gateClearRank = null, xpEarned = null,
            ),
            exercises = exercises.mapIndexed { index, id ->
                CompletionExercisePayload(
                    exerciseId = id,
                    orderIndex = index,
                    notes = null,
                    exerciseScore = null,
                    performanceGrade = null,
                    sets = listOf(
                        CompletionSetPayload(1, 50.0, 10, 8.0, false, true, 66.67, null),
                        CompletionSetPayload(2, 50.0, 10, 8.0, false, true, 66.67, null),
                    ),
                )
            },
        )
        val metadata = exercises.associateWith { id ->
            Exercise(
                id = id,
                name = id.replace('-', ' '),
                category = "back",
                primaryMuscleGroup = "back",
                equipment = if (id == "cable-row") "cable" else "machine",
                exerciseType = "strength",
                rankingEnabled = true,
            )
        }
        val result = computeGateResult(
            payload = payload,
            priorStats = emptyMap(),
            aggregates = CompletionAggregates("Pull Day", null, 3600, 3000.0, 6, 6, 3),
            prRecordTypes = emptyList(),
            exerciseMetadata = metadata,
            bodyweightKg = 71.5,
            sex = "male",
        )

        assertTrue(result.perExercise.all { it.rankingMode == ExerciseRankingMode.UNRANKED })
        assertTrue(result.perExercise.all { it.baselineSessions == 1 && it.exerciseRank == null })
        assertTrue(result.perExercise.all { it.performanceGrade == null })
        assertTrue(result.gateClearRank.ordinal <= Rank.B.ordinal)
    }

    @Test
    fun `single outlier cannot produce an elite personal tier`() {
        val result = calculatePersonalExerciseTier(500.0, listOf(50.0, 51.0))
        assertEquals(ExerciseRankingMode.PERSONAL, result.mode)
        assertTrue(result.rank!!.ordinal <= Rank.C.ordinal)
    }

    @Test
    fun `one elite movement cannot produce elite overall strength`() {
        val result = computeStrengthRank(
            listOf(StrengthAssessmentInput("bench", Equipment.BARBELL, 200.0, 1, sessionId = "one")),
            bodyweightKg = 80.0,
            sex = "male",
        )
        assertTrue(result.rank!!.ordinal <= Rank.C.ordinal)
        assertEquals(AssessmentConfidence.LOW, result.confidence)
    }

    @Test
    fun `Hunter weak link blocks high rank despite one exceptional pillar`() {
        val result = computeHunterRank(99.0, 90.0, 40.0, AssessmentConfidence.HIGH)
        assertTrue(result.rank.ordinal < Rank.A.ordinal)
    }
}


package com.fitnessrpg.app.domain.ranking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    private fun s(
        weightKg: Double? = 100.0,
        reps: Int? = 5,
        rpe: Double? = 8.0,
        isWarmup: Boolean = false,
        isCompleted: Boolean = true,
    ) = RankingSetInput(weightKg, reps, rpe, isWarmup, isCompleted)

    @Test
    fun `accepts a plausible completed working set`() {
        assertTrue(validateWorkingSet(s()).valid)
    }

    @Test
    fun `rejects incomplete and warm-up sets`() {
        assertFalse(validateWorkingSet(s(isCompleted = false)).valid)
        assertFalse(validateWorkingSet(s(isWarmup = true)).valid)
    }

    @Test
    fun `rejects missing load or reps`() {
        assertFalse(validateWorkingSet(s(weightKg = null)).valid)
        assertFalse(validateWorkingSet(s(weightKg = 0.0)).valid)
        assertFalse(validateWorkingSet(s(reps = null)).valid)
    }

    @Test
    fun `rejects implausible weight, reps, and rpe`() {
        assertFalse(validateWorkingSet(s(weightKg = 9999.0)).valid)
        assertFalse(validateWorkingSet(s(reps = 500)).valid)
        assertFalse(validateWorkingSet(s(rpe = 11.0)).valid)
    }

    @Test
    fun `excludes reps above the strength cap of 12`() {
        assertFalse(validateWorkingSet(s(reps = 13)).valid)
    }

    @Test
    fun `gives a reason for every rejection`() {
        assertNotNull(validateWorkingSet(s(isWarmup = true)).reason)
    }

    @Test
    fun `qualifyingWorkingSets keeps only valid sets`() {
        val sets = listOf(s(), s(isWarmup = true), s(weightKg = null), s(reps = 3))
        assertEquals(2, qualifyingWorkingSets(sets).size)
    }

    @Test
    fun `needs at least two qualifying sets`() {
        assertFalse(meetsQualifyingThreshold(1))
        assertTrue(meetsQualifyingThreshold(2))
    }
}

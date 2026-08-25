package com.fitnessrpg.app.domain.rankings

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysiqueScoreTest {

    private fun body(
        bodyFat: Double? = null,
        muscleMass: Double? = null,
        smm: Double? = null,
        lean: Double? = null,
    ) = BodyCompositionData(
        weightKg = 71.5, heightCm = 171.0, bodyFatPercent = bodyFat,
        muscleMassKg = muscleMass, skeletalMuscleMassKg = smm, leanBodyMassKg = lean, sex = "male",
    )

    @Test
    fun `null when no usable body composition data`() {
        assertNull(computePhysiqueScore(BodyCompositionData(weightKg = 71.5, heightCm = 171.0, sex = "male")))
    }

    @Test
    fun `total muscle mass is NOT treated as skeletal muscle mass`() {
        val asMuscleMass = computePhysiqueScore(body(muscleMass = 55.2))!!
        val asSmm = computePhysiqueScore(body(smm = 55.2))!!
        // 55.2/71.5 = 77% is impossible for SMM; treating it as SMM saturates to 100,
        // while the conservative total-muscle-mass path scores clearly lower.
        assertTrue(asSmm > asMuscleMass)
        assertTrue(asMuscleMass < 95.0)
    }

    @Test
    fun `body fat present derives lean mass via FFMI and ignores raw muscle mass`() {
        // With body fat, the muscle-development signal comes from FFMI (lean/height²),
        // NOT the raw 55.2 "muscle mass" number — so the two agree.
        val withMuscleMass = computePhysiqueScore(body(bodyFat = 18.0, muscleMass = 55.2))!!
        val withoutMuscleMass = computePhysiqueScore(body(bodyFat = 18.0))!!
        assertTrue(kotlin.math.abs(withMuscleMass - withoutMuscleMass) < 0.001)
    }

    @Test
    fun `does not reward extremely low body fat over a healthy level`() {
        val healthy = computePhysiqueScore(body(bodyFat = 12.0))!!
        val extreme = computePhysiqueScore(body(bodyFat = 4.0))!!
        assertTrue(extreme < healthy)
    }

    @Test
    fun `the real user physique lands in a moderate range`() {
        val score = computePhysiqueScore(body(bodyFat = 18.0, muscleMass = 55.2))!!
        assertTrue("physique was $score", score in 60.0..80.0)
    }
}

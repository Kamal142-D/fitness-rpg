package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.ranking.Anchor

/** Single configurable source for every Ranking System V3 threshold and weight. */
object RankingV3Config {
    const val VERSION = 3
    const val BODY_ASSESSMENT_VALID_DAYS = 90L
    const val STRENGTH_VALID_DAYS = 60L
    const val CONDITIONING_VALID_DAYS = 90L

    const val HUNTER_PHYSIQUE_WEIGHT = .35
    const val HUNTER_STRENGTH_WEIGHT = .40
    const val HUNTER_CONDITIONING_WEIGHT = .25
    const val HUNTER_BASE_WEIGHT = .80
    const val HUNTER_WEAKEST_WEIGHT = .20

    const val PHYSIQUE_COMPOSITION_WEIGHT = .30
    const val PHYSIQUE_MUSCULARITY_WEIGHT = .35
    const val PHYSIQUE_WAIST_WEIGHT = .25
    const val PHYSIQUE_BALANCE_WEIGHT = .10

    const val STRENGTH_AVERAGE_WEIGHT = .80
    const val STRENGTH_WEAKEST_WEIGHT = .20

    const val GATE_INTENSITY_WEIGHT = .45
    const val GATE_HARD_SETS_WEIGHT = .25
    const val GATE_VOLUME_WEIGHT = .20
    const val GATE_DENSITY_WEIGHT = .10

    const val CLEAR_COMPLETION_WEIGHT = .35
    const val CLEAR_TARGET_WEIGHT = .30
    const val CLEAR_PROGRESS_WEIGHT = .25
    const val CLEAR_PR_WEIGHT = .10

    val hunterRequirements: Map<Rank, RankRequirement> = linkedMapOf(
        Rank.E to RankRequirement(0, 0, 0, 0),
        Rank.D to RankRequirement(15, 10, 10, 10),
        Rank.C to RankRequirement(30, 20, 20, 20),
        Rank.B to RankRequirement(45, 35, 35, 30),
        Rank.A to RankRequirement(60, 50, 50, 45),
        Rank.S to RankRequirement(75, 65, 65, 60),
        Rank.S_PLUS to RankRequirement(85, 75, 75, 70),
        Rank.SS to RankRequirement(90, 85, 85, 80),
        Rank.SSS to RankRequirement(96, 92, 92, 88),
    )

    data class PhysiqueRequirement(
        val overall: Int,
        val composition: Int,
        val muscularity: Int,
        val waist: Int,
        val balance: Int? = null,
    )

    val physiqueRequirements: Map<Rank, PhysiqueRequirement> = linkedMapOf(
        Rank.E to PhysiqueRequirement(0, 0, 0, 0),
        Rank.D to PhysiqueRequirement(15, 10, 10, 10),
        Rank.C to PhysiqueRequirement(30, 20, 20, 20),
        Rank.B to PhysiqueRequirement(45, 35, 35, 30),
        Rank.A to PhysiqueRequirement(60, 50, 50, 45),
        Rank.S to PhysiqueRequirement(75, 65, 65, 60),
        Rank.S_PLUS to PhysiqueRequirement(85, 75, 75, 70),
        Rank.SS to PhysiqueRequirement(90, 82, 85, 80),
        Rank.SSS to PhysiqueRequirement(96, 90, 92, 88, 80),
    )

    const val PERSONAL_BASELINE_SESSIONS = 3
    val personalProgressAnchors = listOf(
        Anchor(.75, 0.0), Anchor(.90, 18.0), Anchor(1.0, 30.0), Anchor(1.05, 45.0),
        Anchor(1.10, 58.0), Anchor(1.18, 72.0), Anchor(1.28, 85.0),
        Anchor(1.40, 93.0), Anchor(1.55, 97.0), Anchor(1.75, 99.0),
    )

    data class AgeBand(val range: IntRange, val bodyFatAllowance: Double, val strengthScale: Double)
    val ageBands = listOf(
        AgeBand(13..39, 0.0, 1.0),
        AgeBand(40..59, 2.0, .95),
        AgeBand(60..120, 4.0, .88),
    )

    val maleBodyFat = listOf(Anchor(4.0, 35.0), Anchor(8.0, 75.0), Anchor(12.0, 100.0), Anchor(16.0, 92.0), Anchor(20.0, 72.0), Anchor(25.0, 48.0), Anchor(35.0, 18.0), Anchor(60.0, 0.0))
    val femaleBodyFat = listOf(Anchor(10.0, 35.0), Anchor(16.0, 75.0), Anchor(21.0, 100.0), Anchor(25.0, 92.0), Anchor(30.0, 70.0), Anchor(38.0, 40.0), Anchor(48.0, 15.0), Anchor(60.0, 0.0))
    val maleFfmi = listOf(Anchor(15.0, 15.0), Anchor(18.0, 42.0), Anchor(20.0, 60.0), Anchor(22.0, 75.0), Anchor(24.0, 88.0), Anchor(26.0, 96.0), Anchor(28.0, 100.0))
    val femaleFfmi = listOf(Anchor(12.0, 15.0), Anchor(15.0, 42.0), Anchor(17.0, 60.0), Anchor(19.0, 75.0), Anchor(21.0, 88.0), Anchor(23.0, 96.0), Anchor(25.0, 100.0))
    val maleSmmPercent = listOf(Anchor(25.0, 15.0), Anchor(32.0, 40.0), Anchor(38.0, 62.0), Anchor(44.0, 82.0), Anchor(50.0, 96.0))
    val femaleSmmPercent = listOf(Anchor(20.0, 15.0), Anchor(27.0, 40.0), Anchor(33.0, 62.0), Anchor(38.0, 82.0), Anchor(44.0, 96.0))
    val waistToHeight = listOf(Anchor(.35, 75.0), Anchor(.40, 100.0), Anchor(.45, 94.0), Anchor(.50, 72.0), Anchor(.55, 45.0), Anchor(.65, 12.0), Anchor(.80, 0.0))
}

package com.fitnessrpg.app.domain.rankings

import com.fitnessrpg.app.domain.rank.Rank

/**
 * Redesigned Hunter ranking domain (PLAN: three physical pillars, weakest-attribute
 * penalty, minimum-requirement gating, provisional cap). Discipline is deliberately
 * NOT a pillar here — it drives XP/level/streaks/quests, never Hunter Rank.
 */

/** The three physical pillars that determine Hunter Rank. */
enum class PhysicalAttribute { PHYSIQUE, STRENGTH, CONDITIONING }

enum class Equipment { BARBELL, DUMBBELL, MACHINE, SMITH_MACHINE, CABLE, BODYWEIGHT, OTHER }

/** For dumbbells, whether the entered weight is per hand or the combined total. */
enum class DumbbellWeightMode { PER_HAND, COMBINED }

/** How much validated physical data backs the assessment. Only HIGH may reach S. */
enum class AssessmentConfidence { LOW, MEDIUM, HIGH }
enum class BodyAssessmentSource { INBODY, SMART_SCALE, MANUAL, OTHER }
enum class ConditioningTestType { COOPER_12_MINUTE, RUN_1_5_MILE, STEP_3_MINUTE }

/**
 * Body-composition inputs. Each field is a DISTINCT measurement — muscleMassKg
 * (e.g. InBody total "Muscle Mass"), skeletalMuscleMassKg (SMM), and leanBodyMassKg
 * are NEVER interchangeable and must never be silently converted into one another.
 */
data class BodyCompositionData(
    val weightKg: Double,
    val heightCm: Double,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val skeletalMuscleMassKg: Double? = null,
    val leanBodyMassKg: Double? = null,
    val visceralFatLevel: Double? = null,
    val waistCm: Double? = null,
    val ageYears: Int? = null,
    val source: BodyAssessmentSource? = null,
    val assessedAtEpochDay: Long? = null,
    /** "male" | "female" | null (neutral). */
    val sex: String? = null,
)

/** A single strength-assessment set. Equipment + reps are required for ranking. */
data class StrengthAssessmentInput(
    /** A movement key (e.g. "bench", "squat") or a catalog exercise name. */
    val exerciseId: String,
    val equipment: Equipment,
    val weightKg: Double,
    val reps: Int,
    /** Required when equipment == DUMBBELL. Defaults to PER_HAND when null. */
    val dumbbellWeightMode: DumbbellWeightMode? = null,
    val variation: String = "standard",
    val rpe: Double? = null,
    val performedAtEpochDay: Long? = null,
)

data class PhysiqueRankResult(val score: Double?, val rank: Rank?, val bodyCompositionScore: Double?, val muscularityScore: Double?, val waistScore: Double?, val balanceScore: Double?, val rankCap: Rank?, val provisional: Boolean, val reasons: List<String>)
data class StrengthRankResult(val score: Double?, val rank: Rank?, val movementScores: Map<String, Double>, val rankCap: Rank?, val provisional: Boolean, val confidence: AssessmentConfidence, val reasons: List<String>)
data class ConditioningRankResult(val score: Double?, val rank: Rank?, val provisional: Boolean, val confidence: AssessmentConfidence, val reasons: List<String>)

/** Minimum thresholds a rank requires across the score + each pillar. */
data class RankRequirement(
    val minHunterScore: Int,
    val minPhysique: Int,
    val minStrength: Int,
    val minConditioning: Int,
)

/** The requirements the NEXT rank up needs, for the "what to improve" UI. */
data class NextRankInfo(
    val rank: Rank,
    val physique: Int,
    val strength: Int,
    val conditioning: Int,
    val hunterScore: Int,
)

/** The full result of a Hunter-rank computation, with explainability. */
data class HunterRankResult(
    val rank: Rank,
    val hunterScore: Double,
    val physiqueScore: Double?,
    val strengthScore: Double?,
    val conditioningScore: Double?,
    val limitingAttribute: PhysicalAttribute?,
    val provisional: Boolean,
    val confidence: AssessmentConfidence,
    val nextRank: NextRankInfo?,
    val rankCap: Rank? = null,
    val reasons: List<String> = emptyList(),
)

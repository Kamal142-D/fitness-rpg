package com.fitnessrpg.app.domain.progression

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.ranking.DisciplineInput
import com.fitnessrpg.app.domain.ranking.PhysiqueInput
import com.fitnessrpg.app.domain.ranking.disciplineScore
import com.fitnessrpg.app.domain.rankings.BodyCompositionData
import com.fitnessrpg.app.domain.rankings.computeHunterRank
import com.fitnessrpg.app.domain.rankings.computePhysiqueScore
import com.fitnessrpg.app.domain.rankings.computeStrengthScoreFromEstimated1RMs

/**
 * Build the durable progression snapshot persisted after a workout. Hunter Rank
 * now comes from the three-pillar engine (physique + strength + conditioning),
 * NOT a weighted average that includes discipline. Discipline is still computed
 * for XP/streaks/quests, but never feeds Hunter Rank.
 *
 * Physique deliberately does NOT read the ambiguous skeletal-muscle-mass column
 * (existing data may hold total muscle mass there); it uses body fat + FFMI.
 */
data class AttributeInputs(
    val strength: Double?,
    val physique: Double?,
    val endurance: Double?,
    val discipline: Double?,
)

data class FinishExercise(val name: String, val best1RMkg: Double?)

data class FinishInputs(
    val bodyweightKg: Double?,
    val heightCm: Double?,
    val sex: String?,
    val exercises: List<FinishExercise>,
    val assessment: PhysiqueInput?,
)

/** Recompute the physical pillars after a workout (pure). Conditioning is not yet
 *  assessable, so it stays unknown (null) — the rank engine treats that as provisional. */
fun computeAttributes(inputs: FinishInputs, newStreakDays: Int): AttributeInputs {
    val strengthItems = inputs.exercises.mapNotNull { e -> e.best1RMkg?.let { e.name to it } }
    val strength = computeStrengthScoreFromEstimated1RMs(strengthItems, inputs.bodyweightKg ?: 0.0, inputs.sex)

    val physique = inputs.assessment?.let { a ->
        computePhysiqueScore(
            BodyCompositionData(
                weightKg = a.weightKg ?: inputs.bodyweightKg ?: 0.0,
                heightCm = inputs.heightCm ?: 0.0,
                bodyFatPercent = a.bodyFatPercent,
                // Never treat the ambiguous stored value as skeletal muscle mass.
                skeletalMuscleMassKg = null,
                sex = inputs.sex,
            ),
        )
    }

    return AttributeInputs(
        strength = strength,
        physique = physique,
        endurance = null,
        discipline = disciplineScore(DisciplineInput(newStreakDays, 1.0)),
    )
}

data class CurrentAttributes(
    val strength: Double,
    val physique: Double,
    val endurance: Double,
    val discipline: Double,
)

data class StreakSnapshot(val current: Int, val longest: Int)

data class ProgressionUpdateInput(
    val current: ProgressionSnapshot,
    val currentAttributes: CurrentAttributes,
    val xpEarned: Int,
    val streak: StreakSnapshot,
    val attributes: AttributeInputs,
)

data class ProgressionPersistPayload(
    val level: Int,
    val currentXp: Int,
    val lifetimeXp: Int,
    val strengthScore: Double,
    val physiqueScore: Double,
    val enduranceScore: Double,
    val disciplineScore: Double,
    val hunterScore: Double,
    val hunterRank: Rank,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
)

fun buildProgressionUpdate(input: ProgressionUpdateInput): ProgressionPersistPayload {
    val xp = applyXp(input.current, input.xpEarned)

    val resolvedStrength = input.attributes.strength ?: input.currentAttributes.strength
    val resolvedPhysique = input.attributes.physique ?: input.currentAttributes.physique
    val resolvedEndurance = input.attributes.endurance ?: input.currentAttributes.endurance
    val resolvedDiscipline = input.attributes.discipline ?: input.currentAttributes.discipline

    // A 0 score means "no data" for the pillar — treat as unknown for the rank engine.
    fun known(v: Double): Double? = if (v > 0.0) v else null
    val hunter = computeHunterRank(
        physiqueScore = known(resolvedPhysique),
        strengthScore = known(resolvedStrength),
        conditioningScore = null,
    )

    return ProgressionPersistPayload(
        level = xp.level,
        currentXp = xp.currentXp,
        lifetimeXp = xp.lifetimeXp,
        strengthScore = resolvedStrength,
        physiqueScore = resolvedPhysique,
        enduranceScore = resolvedEndurance,
        disciplineScore = resolvedDiscipline,
        hunterScore = hunter.hunterScore,
        hunterRank = hunter.rank,
        currentStreakDays = input.streak.current,
        longestStreakDays = input.streak.longest,
    )
}

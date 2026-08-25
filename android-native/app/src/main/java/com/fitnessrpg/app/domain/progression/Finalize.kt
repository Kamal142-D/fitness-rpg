package com.fitnessrpg.app.domain.progression

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.ranking.DisciplineInput
import com.fitnessrpg.app.domain.ranking.disciplineScore

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

/** Workout completion updates discipline only. Physical pillars are assessment-owned. */
fun computeWorkoutAttributes(newStreakDays: Int): AttributeInputs {
    return AttributeInputs(
        strength = null,
        physique = null,
        endurance = null,
        discipline = disciplineScore(DisciplineInput(newStreakDays, 1.0)),
    )
}

data class CurrentAttributes(
    val strength: Double,
    val physique: Double,
    val endurance: Double,
    val discipline: Double,
    val hunterScore: Double = 0.0,
    val hunterRank: Rank = Rank.E,
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

    return ProgressionPersistPayload(
        level = xp.level,
        currentXp = xp.currentXp,
        lifetimeXp = xp.lifetimeXp,
        strengthScore = resolvedStrength,
        physiqueScore = resolvedPhysique,
        enduranceScore = resolvedEndurance,
        disciplineScore = resolvedDiscipline,
        // Workout completion changes XP/streaks and raw evidence. Hunter Rank is
        // recalculated only by the assessment pipeline, never from lifetime PRs.
        hunterScore = input.currentAttributes.hunterScore,
        hunterRank = input.currentAttributes.hunterRank,
        currentStreakDays = input.streak.current,
        longestStreakDays = input.streak.longest,
    )
}

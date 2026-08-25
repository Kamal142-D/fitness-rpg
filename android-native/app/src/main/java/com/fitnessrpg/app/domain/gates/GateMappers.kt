package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.TemplateExercise
import com.fitnessrpg.app.domain.rank.Rank

/** Pure display mappers for Gates. No I/O — easy to unit test. */

private val DEFAULT_DIFFICULTY = Rank.D
private const val DEFAULT_DURATION = 45

/** A qualitative intensity label for a Gate Difficulty. */
fun intensityForDifficulty(difficulty: Rank): String = when (difficulty) {
    Rank.E, Rank.D -> "Light"
    Rank.C -> "Moderate"
    Rank.B -> "Hard"
    Rank.A, Rank.S -> "Brutal"
}

/** Coerce a possibly-null template difficulty string to a valid Rank. */
fun templateDifficulty(difficulty: String?): Rank = when (difficulty) {
    "E" -> Rank.E
    "D" -> Rank.D
    "C" -> Rank.C
    "B" -> Rank.B
    "A" -> Rank.A
    "S" -> Rank.S
    else -> DEFAULT_DIFFICULTY
}

fun templateDifficulty(t: GateTemplate): Rank = templateDifficulty(t.difficulty)

/** Split a template description into muscle-group chips. */
fun muscleGroupsFor(description: String?): List<String> {
    if (description == null) return emptyList()
    return description.split(Regex("[,·]")).map { it.trim() }.filter { it.isNotEmpty() }
}

/** Map a template to the SuggestedGate shape used by the dashboard GateCard. */
fun templateToSuggestedGate(t: GateTemplate): SuggestedGate {
    val difficulty = templateDifficulty(t)
    return SuggestedGate(
        name = t.name,
        difficulty = difficulty,
        muscleGroups = muscleGroupsFor(t.description),
        durationMinutes = t.estimatedDurationMinutes ?: DEFAULT_DURATION,
        intensity = intensityForDifficulty(difficulty),
    )
}

/** Format a target rep range, e.g. "5-8", "8+", or "—" when unset (time-based). */
fun formatRepRange(min: Int?, max: Int?): String {
    if (min == null && max == null) return "—"
    if (min != null && max != null) return if (min == max) "$min" else "$min-$max"
    if (min != null) return "$min+"
    return "up to $max"
}

/** Format a template exercise's targets, e.g. "4 × 5-8". */
fun formatTargets(targetSets: Int?, targetRepsMin: Int?, targetRepsMax: Int?): String {
    val sets = targetSets ?: 0
    val reps = formatRepRange(targetRepsMin, targetRepsMax)
    if (sets <= 0) return if (reps == "—") "—" else reps
    return if (reps == "—") "$sets sets" else "$sets × $reps"
}

fun formatTargets(te: TemplateExercise): String =
    formatTargets(te.targetSets, te.targetRepsMin, te.targetRepsMax)

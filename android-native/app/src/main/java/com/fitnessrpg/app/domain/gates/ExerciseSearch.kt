package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.model.Exercise

data class ExerciseFilters(
    val muscle: String? = null,
    val equipment: String? = null,
    val category: String? = null,
)

fun searchExercises(
    exercises: List<Exercise>,
    query: String,
    filters: ExerciseFilters = ExerciseFilters(),
): List<Exercise> {
    val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    return exercises.asSequence().filter { exercise ->
        val haystack = buildList {
            add(exercise.name)
            addAll(exercise.aliases)
            exercise.primaryMuscleGroup?.let(::add)
            addAll(exercise.secondaryMuscleGroups)
            exercise.targetMuscle?.let(::add)
            exercise.bodyPart?.let(::add)
            exercise.equipment?.let(::add)
            exercise.category?.let(::add)
        }.joinToString(" ").lowercase()
        terms.all(haystack::contains) &&
            (filters.muscle == null || haystack.contains(filters.muscle.lowercase())) &&
            (filters.equipment == null || exercise.equipment.equals(filters.equipment, ignoreCase = true)) &&
            (filters.category == null || exercise.category.equals(filters.category, ignoreCase = true))
    }.sortedBy { it.name.lowercase() }.toList()
}

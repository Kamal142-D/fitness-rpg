package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.rank.Rank

/** Draft shape for validation (difficulty may be unset in the form). */
data class CreateGateDraft(
    val name: String,
    val difficulty: Rank?,
    val exerciseIds: List<String>,
)

data class CreateGateErrors(
    val name: String? = null,
    val difficulty: String? = null,
    val exercises: String? = null,
) {
    fun hasErrors(): Boolean = name != null || difficulty != null || exercises != null
}

/** Validate custom-Gate creation input. */
fun validateCreateGate(input: CreateGateDraft): CreateGateErrors {
    val name = input.name.trim()
    val nameErr = when {
        name.isEmpty() -> "Name your Gate"
        name.length > 60 -> "Keep the name under 60 characters"
        else -> null
    }
    val difficultyErr = if (input.difficulty == null) "Choose a difficulty" else null
    val exercisesErr = when {
        input.exerciseIds.size < 1 -> "Add at least one exercise"
        input.exerciseIds.size > 15 -> "Keep it to 15 exercises or fewer"
        else -> null
    }
    return CreateGateErrors(nameErr, difficultyErr, exercisesErr)
}

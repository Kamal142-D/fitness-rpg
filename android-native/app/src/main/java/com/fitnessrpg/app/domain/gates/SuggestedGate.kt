package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.rank.Rank

data class SuggestedGate(
    val name: String,
    /** Last assessed post-workout difficulty; null until this user completes it. */
    val difficulty: Rank?,
    val muscleGroups: List<String>,
    val durationMinutes: Int,
    val intensity: String,
)

/** Placeholder "Today's Gate" starter suggestion for the System dashboard. */
val STARTER_GATE = SuggestedGate(
    name = "Full Body — Initiation",
    difficulty = null,
    muscleGroups = listOf("Chest", "Back", "Legs", "Core"),
    durationMinutes = 45,
    intensity = "Not Assessed",
)

package com.fitnessrpg.app.domain.pr

enum class RecordType(val wire: String) {
    WEIGHT("weight"),
    REPS("reps"),
    ESTIMATED_1RM("estimated_1rm"),
    VOLUME("volume"),
}

/** A user's prior all-time bests for one exercise (from exercise_user_stats). */
data class PriorStat(
    val bestWeightKg: Double?,
    val bestReps: Double?,
    val bestEstimated1rmKg: Double?,
    val bestVolumeKg: Double?,
)

data class DetectSet(
    val setNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val est1RM: Double?,
    val isWarmup: Boolean,
)

data class DetectExercise(
    val exerciseId: String,
    val orderIndex: Int,
    val sets: List<DetectSet>,
)

data class DetectedPR(
    val exerciseId: String,
    val orderIndex: Int,
    /** Set that achieved the record (for linking workout_set_id server-side). */
    val setNumber: Int,
    val recordType: RecordType,
    val previousValue: Double?,
    val newValue: Double,
)

/** All-time bests for an exercise after this workout (for exercise_user_stats). */
data class NewStat(
    val exerciseId: String,
    val bestWeightKg: Double?,
    val bestReps: Double?,
    val bestEstimated1rmKg: Double?,
    val bestVolumeKg: Double?,
)

data class DetectResult(val prs: List<DetectedPR>, val stats: List<NewStat>)

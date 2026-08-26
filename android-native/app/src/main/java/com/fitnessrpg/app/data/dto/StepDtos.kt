package com.fitnessrpg.app.data.dto

import com.fitnessrpg.app.domain.steps.DailyStepProgress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyStepDto(
    @SerialName("user_id") val userId: String,
    @SerialName("step_date") val stepDate: String,
    val steps: Int,
    val goal: Int,
    @SerialName("reward_claimed") val rewardClaimed: Boolean = false,
    @SerialName("xp_awarded") val xpAwarded: Int = 0,
) {
    fun toDomain() = DailyStepProgress(
        date = LocalDate.parse(stepDate),
        steps = steps,
        goal = goal,
        rewardClaimed = rewardClaimed,
        xpAwarded = xpAwarded,
    )
}

@Serializable
data class SyncDailyStepsParams(
    @SerialName("p_step_date") val stepDate: String,
    @SerialName("p_steps") val steps: Int,
    @SerialName("p_goal") val goal: Int,
)

@Serializable
data class ClaimDailyStepsParams(
    @SerialName("p_step_date") val stepDate: String,
)

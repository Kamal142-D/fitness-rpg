package com.fitnessrpg.app.domain.plan

import kotlinx.serialization.Serializable

enum class SlotKind { WORKOUT, REST }

/** One position in the training cycle: a workout label (optionally mapped to a Gate) or a rest. */
@Serializable
data class PlanSlot(
    val label: String,
    val kind: SlotKind,
    val gateTemplateId: String? = null,
)

/**
 * A rolling training cycle. [currentIndex] points at today's slot and only moves
 * when the user trains (workouts) or when a rest day rolls past. [startedEpochDay]
 * is the day the current slot became active — used to detect a missed workout.
 */
@Serializable
data class TrainingPlan(
    val slots: List<PlanSlot>,
    val currentIndex: Int = 0,
    val startedEpochDay: Long? = null,
) {
    val current: PlanSlot? get() = if (slots.isEmpty()) null else slots[currentIndex.mod(slots.size)]
    val next: PlanSlot? get() = if (slots.isEmpty()) null else slots[(currentIndex + 1).mod(slots.size)]
}

/** The user's default split: push, pull, legs & abs, rest, full upper, full lower, rest. */
val DEFAULT_PLAN_SLOTS: List<PlanSlot> = listOf(
    PlanSlot("Push", SlotKind.WORKOUT),
    PlanSlot("Pull", SlotKind.WORKOUT),
    PlanSlot("Legs & Abs", SlotKind.WORKOUT),
    PlanSlot("Rest", SlotKind.REST),
    PlanSlot("Full Upper", SlotKind.WORKOUT),
    PlanSlot("Full Lower", SlotKind.WORKOUT),
    PlanSlot("Rest", SlotKind.REST),
)

fun defaultPlan(today: Long? = null): TrainingPlan = TrainingPlan(DEFAULT_PLAN_SLOTS, 0, today)

enum class PlanState { TODAY_WORKOUT, TODAY_REST, MISSED_WORKOUT }

data class PlanStatus(val state: PlanState, val current: PlanSlot, val next: PlanSlot, val daysOverdue: Long)

/** Move to the next slot, marking today as the day it became active. */
fun TrainingPlan.advanced(today: Long?): TrainingPlan {
    if (slots.isEmpty()) return this
    return copy(currentIndex = (currentIndex + 1).mod(slots.size), startedEpochDay = today)
}

/** Keep today's slot but reset its clock, so a missed workout becomes "due today" again. */
fun TrainingPlan.renewedToday(today: Long): TrainingPlan = copy(startedEpochDay = today)

/**
 * Roll forward past any rest slot whose day has already passed. Rest is one day; the
 * user never sits on a stale rest. Returns the plan unchanged if nothing to roll.
 */
fun TrainingPlan.normalized(today: Long): TrainingPlan {
    if (slots.isEmpty()) return this
    var plan = this
    var guard = 0
    while (plan.current?.kind == SlotKind.REST &&
        (today - (plan.startedEpochDay ?: today)) >= 1L &&
        guard < plan.slots.size
    ) {
        plan = plan.advanced(today)
        guard++
    }
    return plan
}

/** Today's plan status: a workout to do, a rest, or a workout you're overdue on. */
fun TrainingPlan.status(today: Long): PlanStatus? {
    val cur = current ?: return null
    val nxt = next ?: cur
    val overdue = today - (startedEpochDay ?: today)
    val state = when {
        cur.kind == SlotKind.REST -> PlanState.TODAY_REST
        overdue >= 1L -> PlanState.MISSED_WORKOUT
        else -> PlanState.TODAY_WORKOUT
    }
    return PlanStatus(state, cur, nxt, if (state == PlanState.MISSED_WORKOUT) overdue else 0L)
}

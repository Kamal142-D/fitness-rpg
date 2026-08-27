package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.cache.PersistentCache
import com.fitnessrpg.app.domain.plan.TrainingPlan
import com.fitnessrpg.app.domain.plan.advanced
import com.fitnessrpg.app.domain.plan.defaultPlan
import com.fitnessrpg.app.domain.plan.normalized
import com.fitnessrpg.app.domain.plan.renewedToday
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Stores the user's training cycle on-device (via [PersistentCache]) — it's personal
 * config that must work offline and never blocks the network. Every read normalizes
 * the plan (rolling past finished rest days) and persists the result.
 */
class TrainingPlanRepository {

    private var cached: TrainingPlan? = null

    private fun today(): Long = LocalDate.now(ZoneOffset.UTC).toEpochDay()

    private fun key(userId: String) = "plan:$userId"

    /** Today's plan, normalized. Seeds the default split the first time. */
    suspend fun get(userId: String): TrainingPlan {
        val loaded = cached
            ?: PersistentCache.load(key(userId), TrainingPlan.serializer())?.first
            ?: defaultPlan(today())
        val normalized = loaded.normalized(today())
        if (normalized != loaded || cached == null) save(userId, normalized)
        return normalized
    }

    suspend fun save(userId: String, plan: TrainingPlan) {
        cached = plan
        PersistentCache.save(key(userId), plan, TrainingPlan.serializer())
    }

    /** Advance one slot (used on workout completion and on "skip to next"). */
    suspend fun advance(userId: String): TrainingPlan {
        val next = get(userId).advanced(today())
        save(userId, next)
        return next
    }

    /** Keep today's slot but make it due today again ("do it now" on a missed workout). */
    suspend fun renewToday(userId: String): TrainingPlan {
        val next = get(userId).renewedToday(today())
        save(userId, next)
        return next
    }

    /** Map (or clear) the Gate used for the workout slot at [index]. */
    suspend fun setSlotGate(userId: String, index: Int, gateTemplateId: String?): TrainingPlan {
        val plan = get(userId)
        if (index !in plan.slots.indices) return plan
        val slots = plan.slots.toMutableList()
        slots[index] = slots[index].copy(gateTemplateId = gateTemplateId)
        val next = plan.copy(slots = slots)
        save(userId, next)
        return next
    }

    /** Reset to the default split (used by the plan editor's reset). */
    suspend fun resetToDefault(userId: String): TrainingPlan {
        val next = defaultPlan(today())
        save(userId, next)
        return next
    }

    /** Best-effort advance after a finished workout; never throws into the finish flow. */
    suspend fun advanceOnFinish(userId: String) {
        runCatching { advance(userId) }
    }
}

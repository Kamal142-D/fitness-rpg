package com.fitnessrpg.app.util

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private val counter = AtomicInteger(0)

/**
 * Generate a locally-unique id for client-side entities (active-workout sets,
 * exercises). Not a UUID — good enough for in-memory / persisted local state.
 */
fun genId(prefix: String = "id"): String {
    val n = (counter.incrementAndGet() % 1_000_000)
    val time = System.currentTimeMillis().toString(36)
    return "${prefix}_${time}_${n.toString(36)}"
}

/**
 * RFC-4122 v4 UUID, used as the client-generated session id so completing a
 * workout is idempotent (a retry reuses the same id).
 */
fun uuidV4(): String = UUID.randomUUID().toString()

package com.fitnessrpg.app.data.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-lifetime in-memory cache for screen data, keyed by a string. Because it
 * lives OUTSIDE composition, cached values survive tab switches and back-navigation
 * — so a screen can render its last value instantly and refresh in the background
 * (stale-while-revalidate), instead of re-fetching from the network every time.
 *
 * Values are expected to be non-null result holders (lists, data classes).
 */
object DataCache {
    private data class Entry(val value: Any?, val loadedAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    /** The cached value for [key], or null if nothing is cached. */
    fun peek(key: String): Any? = entries[key]?.value

    /** Age of the cached value in ms, or null if nothing is cached. */
    fun ageMs(key: String): Long? = entries[key]?.let { System.currentTimeMillis() - it.loadedAt }

    fun put(key: String, value: Any?) {
        entries[key] = Entry(value, System.currentTimeMillis())
    }

    fun invalidate(key: String) {
        entries.remove(key)
    }

    /** Drop every cached entry whose key starts with [prefix] (e.g. "player:"). */
    fun invalidatePrefix(prefix: String) {
        entries.keys.removeAll { it.startsWith(prefix) }
    }

    /** Clear everything — call on sign-out so no data bleeds between accounts. */
    fun clear() {
        entries.clear()
    }
}

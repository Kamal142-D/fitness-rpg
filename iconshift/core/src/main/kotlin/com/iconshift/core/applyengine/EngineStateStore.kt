package com.iconshift.core.applyengine

/** Small persistent blob store engines use for backups and bookkeeping. */
interface EngineStateStore {
    fun get(key: String): ByteArray?
    fun put(key: String, value: ByteArray)
    fun remove(key: String)
}

class InMemoryEngineStateStore : EngineStateStore {
    private val map = HashMap<String, ByteArray>()
    override fun get(key: String): ByteArray? = map[key]
    override fun put(key: String, value: ByteArray) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}

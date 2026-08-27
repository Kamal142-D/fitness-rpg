package com.fitnessrpg.app.data.cache

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Disk backing for [DataCache]: stores each screen's data as a JSON snapshot in a
 * small Room database, so a cold app start (or an offline device) can paint the
 * last-known screen instantly and then refresh from the network when possible.
 */
object PersistentCache {
    private var dao: CacheDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun init(context: Context) {
        if (dao != null) return
        dao = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "fitnessrpg-cache.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
            .cacheDao()
    }

    val isReady: Boolean get() = dao != null

    /** Load and decode the snapshot for [key], returning value + when it was saved. */
    suspend fun <T> load(key: String, serializer: KSerializer<T>): Pair<T, Long>? {
        val d = dao ?: return null
        return runCatching {
            val entity = d.get(key) ?: return null
            json.decodeFromString(serializer, entity.json) to entity.updatedAt
        }.getOrNull()
    }

    /** Encode and persist [value] under [key]. */
    suspend fun <T> save(key: String, value: T, serializer: KSerializer<T>) {
        val d = dao ?: return
        runCatching { d.upsert(CacheEntity(key, json.encodeToString(serializer, value), System.currentTimeMillis())) }
    }

    fun deletePrefixAsync(prefix: String) {
        val d = dao ?: return
        scope.launch { runCatching { d.deleteLike("$prefix%") } }
    }

    fun clearAsync() {
        val d = dao ?: return
        scope.launch { runCatching { d.clearAll() } }
    }
}

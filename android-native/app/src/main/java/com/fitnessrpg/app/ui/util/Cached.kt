package com.fitnessrpg.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fitnessrpg.app.data.cache.DataCache
import com.fitnessrpg.app.data.cache.PersistentCache
import kotlinx.serialization.KSerializer

/** Observable result of a cached load. [data] is the last known value (from memory,
 *  or hydrated from disk on a cold start); [loading] is true only when there is
 *  nothing cached to show yet. */
class CachedState<T>(
    val data: T?,
    val loading: Boolean,
    val refreshing: Boolean,
    val error: Throwable?,
    val refresh: () -> Unit,
)

/**
 * Stale-while-revalidate loader with optional on-disk persistence.
 *
 * Order on entry: in-memory value → (if [serializer] given) the Room snapshot from
 * a previous session → network refresh. So switching tabs is instant (memory), a
 * cold start paints from disk almost instantly and works offline, and fresh data
 * arrives in the background. [refresh] forces a reload (e.g. after a mutation).
 */
@Composable
fun <T> rememberCached(
    key: String,
    serializer: KSerializer<T>? = null,
    ttlMs: Long = 30_000L,
    loader: suspend () -> T,
): CachedState<T> {
    @Suppress("UNCHECKED_CAST")
    var data by remember(key) { mutableStateOf(DataCache.peek(key) as? T) }
    var refreshing by remember(key) { mutableStateOf(false) }
    var error by remember(key) { mutableStateOf<Throwable?>(null) }
    var forceCount by remember(key) { mutableIntStateOf(0) }

    LaunchedEffect(key, forceCount) {
        // 1. Cold start: nothing in memory but a snapshot may exist on disk.
        if (data == null && serializer != null && PersistentCache.isReady) {
            PersistentCache.load(key, serializer)?.let { (value, savedAt) ->
                data = value
                DataCache.putWithTime(key, value, savedAt)
            }
        }

        // 2. Refresh from the network if forced, empty, or stale.
        val age = DataCache.ageMs(key)
        val stale = age == null || age > ttlMs
        if (forceCount > 0 || data == null || stale) {
            refreshing = true
            error = null
            runCatching { loader() }
                .onSuccess { result ->
                    DataCache.put(key, result)
                    data = result
                    if (serializer != null && PersistentCache.isReady) PersistentCache.save(key, result, serializer)
                }
                .onFailure { error = it }
            refreshing = false
        }
    }

    return CachedState(
        data = data,
        loading = data == null && refreshing,
        refreshing = refreshing,
        // Only surface an error when there is nothing cached to show instead
        // (offline with a saved snapshot keeps showing the snapshot).
        error = if (data == null) error else null,
        refresh = { forceCount++ },
    )
}

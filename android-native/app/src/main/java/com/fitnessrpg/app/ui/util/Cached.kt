package com.fitnessrpg.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fitnessrpg.app.data.cache.DataCache

/** Observable result of a cached load. [data] is the last known value (possibly
 *  cached from a previous visit); [loading] is true only on the first-ever load
 *  when there is nothing cached to show. */
class CachedState<T>(
    val data: T?,
    val loading: Boolean,
    val refreshing: Boolean,
    val error: Throwable?,
    val refresh: () -> Unit,
)

/**
 * Stale-while-revalidate loader. On entry it shows the cached value immediately
 * (no spinner), then refreshes in the background only if the cache is missing or
 * older than [ttlMs]. [refresh] forces a reload (e.g. after a mutation). The cache
 * lives in [DataCache], so switching tabs and coming back is instant.
 */
@Composable
fun <T> rememberCached(
    key: String,
    ttlMs: Long = 30_000L,
    loader: suspend () -> T,
): CachedState<T> {
    @Suppress("UNCHECKED_CAST")
    var data by remember(key) { mutableStateOf(DataCache.peek(key) as? T) }
    var refreshing by remember(key) { mutableStateOf(false) }
    var error by remember(key) { mutableStateOf<Throwable?>(null) }
    var forceCount by remember(key) { mutableIntStateOf(0) }

    LaunchedEffect(key, forceCount) {
        val age = DataCache.ageMs(key)
        val stale = age == null || age > ttlMs
        if (forceCount > 0 || data == null || stale) {
            refreshing = true
            error = null
            runCatching { loader() }
                .onSuccess { result ->
                    DataCache.put(key, result)
                    data = result
                }
                .onFailure { error = it }
            refreshing = false
        }
    }

    return CachedState(
        data = data,
        loading = data == null && refreshing,
        refreshing = refreshing,
        // Only surface an error when there is nothing cached to show instead.
        error = if (data == null) error else null,
        refresh = { forceCount++ },
    )
}

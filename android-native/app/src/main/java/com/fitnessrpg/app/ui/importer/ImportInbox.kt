package com.fitnessrpg.app.ui.importer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds a LiftoffRank link shared into the app from the Android share sheet (or a
 * deep link) until the UI is ready to consume it. Set by MainActivity, observed by
 * the nav host, cleared once the import screen opens.
 */
object ImportInbox {
    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl

    /** Accept a shared URL if it contains an importable link. */
    fun offer(url: String?) {
        val trimmed = url?.trim().orEmpty()
        val link = LINK_REGEX.find(trimmed)?.value ?: return
        _pendingUrl.value = link
    }

    fun consume(): String? = _pendingUrl.value.also { _pendingUrl.value = null }

    fun clear() { _pendingUrl.value = null }

    private val LINK_REGEX = Regex("""https://\S*liftoffrank\.com/\S+""")
}

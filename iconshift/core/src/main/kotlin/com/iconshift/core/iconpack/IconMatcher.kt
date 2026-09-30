package com.iconshift.core.iconpack

import java.text.Normalizer
import java.util.Locale

/** Smart matching (spec §10) and search (spec §11) over an [IconPackIndex]. */
object IconMatcher {

    private val MARKS = Regex("\\p{M}+")
    private val NON_ALNUM = Regex("[^\\p{L}\\p{Nd}]")
    private val SEPARATORS = Regex("[^\\p{L}\\p{Nd}]+")
    private val GENERIC_SEGMENTS = setOf("android", "app", "apps", "mobile", "main", "client", "launcher", "lite", "pro")

    /**
     * Likely icons for an app, best first:
     * 1. exact component mapping, 2. any mapping for the package, 3. drawable name contains the
     * app label, 4. drawable name contains the package's last segment, 5. token-overlap fuzzy match.
     */
    fun recommend(
        index: IconPackIndex,
        packageName: String,
        activityName: String?,
        appLabel: String,
        limit: Int = 30,
    ): List<IconEntry> {
        val out = LinkedHashSet<String>()

        activityName?.let { index.componentToDrawables[ComponentKey.of(packageName, it)] }?.let(out::addAll)
        for ((component, names) in index.componentToDrawables) {
            if (component.substringBefore('/') == packageName) out.addAll(names)
        }

        val entries = index.entries
        val names = index.normalizedNames
        val label = normalize(appLabel)
        val lastSegment = normalize(packageName.substringAfterLast('.'))

        if (label.length >= 3) names.forEachIndexed { i, n -> if (n.contains(label)) out += entries[i].drawableName }
        if (lastSegment.length >= 3 && lastSegment !in GENERIC_SEGMENTS) {
            names.forEachIndexed { i, n -> if (n.contains(lastSegment)) out += entries[i].drawableName }
        }

        val wanted = (tokens(appLabel) + tokens(packageName.substringAfterLast('.'))).filter { it.length >= 3 }.toSet()
        if (wanted.isNotEmpty()) {
            index.nameTokens
                .mapIndexedNotNull { i, nameTokens ->
                    val score = nameTokens.count { t -> wanted.any { t.startsWith(it) || it.startsWith(t) } }
                    if (score > 0) i to score else null
                }
                .sortedByDescending { it.second }
                .forEach { out += entries[it.first].drawableName }
        }

        return out.asSequence().mapNotNull(index::entry).take(limit).toList()
    }

    /** Case/accent-insensitive search on drawable name; also matches mapped package names. */
    fun search(index: IconPackIndex, query: String): List<IconEntry> {
        val q = normalize(query)
        if (q.isEmpty()) return index.entries
        val keys = index.searchKeys
        return index.entries.filterIndexed { i, _ -> keys[i].contains(q) }
    }

    /** Lowercase, strip accents, drop everything but letters/digits. */
    fun normalize(s: String): String {
        // Drawable names are ASCII (a-z0-9_), and packs hold tens of thousands: skip Unicode work for them.
        if (isAscii(s)) {
            val sb = StringBuilder(s.length)
            for (c in s) if (c.isLetterOrDigit()) sb.append(c.lowercaseChar())
            return sb.toString()
        }
        return NON_ALNUM.replace(MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase(Locale.ROOT), "")
    }

    fun tokens(s: String): List<String> {
        if (isAscii(s)) {
            val out = ArrayList<String>(4)
            val sb = StringBuilder()
            for (c in s) {
                if (c.isLetterOrDigit()) {
                    sb.append(c.lowercaseChar())
                } else if (sb.isNotEmpty()) {
                    out += sb.toString()
                    sb.setLength(0)
                }
            }
            if (sb.isNotEmpty()) out += sb.toString()
            return out
        }
        return MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT)
            .split(SEPARATORS)
            .filter { it.isNotEmpty() }
    }

    private fun isAscii(s: String): Boolean {
        for (c in s) if (c.code > 0x7F) return false
        return true
    }
}

package com.iconshift.core.iconpack

import java.text.Normalizer
import java.util.Locale

/** Smart matching (spec §10) and search (spec §11) over an [IconPackIndex]. */
object IconMatcher {

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
        index.componentToDrawables.filterKeys { it.substringBefore('/') == packageName }.values.forEach(out::addAll)

        val label = normalize(appLabel)
        val lastSegment = normalize(packageName.substringAfterLast('.'))
        val names = index.entries.map { it.drawableName to normalize(it.drawableName) }

        if (label.length >= 3) names.filter { it.second.contains(label) }.forEach { out += it.first }
        if (lastSegment.length >= 3 && lastSegment !in GENERIC_SEGMENTS) {
            names.filter { it.second.contains(lastSegment) }.forEach { out += it.first }
        }

        val tokens = (tokens(appLabel) + tokens(packageName.substringAfterLast('.'))).filter { it.length >= 3 }.toSet()
        if (tokens.isNotEmpty()) {
            index.entries
                .map { e ->
                    val nameTokens = tokens(e.drawableName).filter { it.length >= 3 }
                    e.drawableName to nameTokens.count { t -> tokens.any { t.startsWith(it) || it.startsWith(t) } }
                }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .forEach { out += it.first }
        }

        return out.asSequence().mapNotNull(index::entry).take(limit).toList()
    }

    /** Case/accent-insensitive search on drawable name and label; also matches mapped package names. */
    fun search(index: IconPackIndex, query: String): List<IconEntry> {
        val q = normalize(query)
        if (q.isEmpty()) return index.entries
        return index.entries.filter { e ->
            normalize(e.drawableName).contains(q) ||
                e.mappedComponents.any { normalize(it.substringBefore('/')).contains(q) }
        }
    }

    /** Lowercase, strip accents, drop everything but letters/digits. */
    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{Nd}]"), "")

    private fun tokens(s: String): List<String> =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotEmpty() }

    private val GENERIC_SEGMENTS = setOf("android", "app", "apps", "mobile", "main", "client", "launcher", "lite", "pro")
}

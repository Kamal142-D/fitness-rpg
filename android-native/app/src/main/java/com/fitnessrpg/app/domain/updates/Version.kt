package com.fitnessrpg.app.domain.updates

/**
 * Semantic-ish version comparison. Pure. Tolerates a leading "v" and differing
 * part counts (e.g. "1.2" vs "1.2.0"). Prerelease/build suffixes are ignored.
 */
fun parseVersion(v: String): List<Int> {
    val cleaned = v.trim().replace(Regex("^v", RegexOption.IGNORE_CASE), "")
    val core = cleaned.split(Regex("[-+]"))[0] // drop prerelease/build metadata
    return core.split(".").map { part -> part.toIntOrNull() ?: 0 }
}

/** -1 if a<b, 0 if equal, 1 if a>b. */
fun compareVersions(a: String, b: String): Int {
    val pa = parseVersion(a)
    val pb = parseVersion(b)
    val len = maxOf(pa.size, pb.size)
    for (i in 0 until len) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x > y) return 1
        if (x < y) return -1
    }
    return 0
}

/** True when [latest] is strictly newer than [current]. */
fun isNewerVersion(latest: String, current: String): Boolean = compareVersions(latest, current) > 0

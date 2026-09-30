package com.iconshift.core.iconpack

/** An installed icon pack app. [iconCount] is null until the pack has been indexed. */
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val iconCount: Int? = null,
)

/** One drawable offered by a pack, with the app components the pack maps it to (from appfilter.xml). */
data class IconEntry(
    val drawableName: String,
    val label: String,
    val mappedComponents: List<String>,
)

/** Parsed contents of one pack: every drawable name plus the component -> drawable mapping. */
class IconPackIndex(
    val packPackage: String,
    val entries: List<IconEntry>,
    /** Keyed by "pkg/cls" (normalised, see [ComponentKey]). */
    val componentToDrawables: Map<String, List<String>>,
) {
    private val byName = entries.associateBy { it.drawableName }

    fun entry(drawableName: String): IconEntry? = byName[drawableName]

    val mappedPackages: Set<String> by lazy {
        componentToDrawables.keys.map { it.substringBefore('/') }.toSet()
    }

    /** [IconMatcher.normalize]d drawable names, parallel to [entries]. Computed once per index. */
    val normalizedNames: List<String> by lazy { entries.map { IconMatcher.normalize(it.drawableName) } }

    /** Search keys parallel to [entries]: normalised name plus mapped package names. */
    val searchKeys: List<String> by lazy {
        entries.mapIndexed { i, e ->
            if (e.mappedComponents.isEmpty()) {
                normalizedNames[i]
            } else {
                normalizedNames[i] + "|" + e.mappedComponents.joinToString("|") { IconMatcher.normalize(it.substringBefore('/')) }
            }
        }
    }

    /** Word tokens of each drawable name (3+ chars), parallel to [entries]; used for fuzzy matching. */
    val nameTokens: List<List<String>> by lazy {
        entries.map { e -> IconMatcher.tokens(e.drawableName).filter { it.length >= 3 } }
    }
}

/** appfilter.xml writes components as `ComponentInfo{com.pkg/com.pkg.Activity}`; normalised to `pkg/cls`. */
object ComponentKey {
    fun parse(raw: String): String? {
        val inner = raw.trim().removePrefix("ComponentInfo{").removeSuffix("}").trim()
        val pkg = inner.substringBefore('/', "")
        var cls = inner.substringAfter('/', "")
        if (pkg.isBlank() || cls.isBlank()) return null
        if (cls.startsWith(".")) cls = pkg + cls
        return "$pkg/$cls"
    }

    fun of(packageName: String, activityName: String): String {
        val cls = if (activityName.startsWith(".")) packageName + activityName else activityName
        return "$packageName/$cls"
    }
}

package com.iconshift.core.iconpack

/**
 * Plain-text cache format for a parsed [IconPackIndex], so a pack is only parsed once per
 * installed version. Drawable names and components never contain tabs or newlines.
 *
 * ```
 * ICONSHIFT-INDEX\t1
 * D\t<drawable>[\t<component>...]
 * M\t<component>\t<drawable>[\t<drawable>...]
 * ```
 */
object IconPackIndexCodec {
    private const val HEADER = "ICONSHIFT-INDEX\t1"

    fun encode(index: IconPackIndex): String = buildString {
        appendLine(HEADER)
        for (e in index.entries) {
            append("D\t").append(e.drawableName)
            for (c in e.mappedComponents) append('\t').append(c)
            append('\n')
        }
        for ((component, names) in index.componentToDrawables) {
            append("M\t").append(component)
            for (n in names) append('\t').append(n)
            append('\n')
        }
    }

    /** Null when [text] is not a cache file of this version (then the pack is re-parsed). */
    fun decode(packPackage: String, text: String): IconPackIndex? {
        val lines = text.lineSequence().iterator()
        if (!lines.hasNext() || lines.next() != HEADER) return null
        val entries = ArrayList<IconEntry>()
        val mappings = LinkedHashMap<String, List<String>>()
        for (line in lines) {
            if (line.isEmpty()) continue
            val parts = line.split('\t')
            when (parts[0]) {
                "D" -> if (parts.size >= 2) {
                    entries += IconEntry(parts[1], IconPackIndexBuilder.labelFor(parts[1]), parts.drop(2))
                }
                "M" -> if (parts.size >= 3) mappings[parts[1]] = parts.drop(2)
                else -> return null
            }
        }
        return IconPackIndex(packPackage, entries, mappings)
    }
}

package com.iconshift.core.iconpack

/**
 * Collects items from a pack's appfilter.xml and drawable.xml (parsed on device) into an
 * [IconPackIndex]. The XML parsing itself stays on the Android side; this keeps the rules testable.
 */
class IconPackIndexBuilder(private val packPackage: String) {

    private val order = LinkedHashSet<String>()
    private val mappings = LinkedHashMap<String, MutableList<String>>()
    private val componentsByDrawable = HashMap<String, MutableList<String>>()

    /** `<item component="ComponentInfo{pkg/cls}" drawable="name"/>` from appfilter.xml. */
    fun onAppFilterItem(component: String?, drawable: String?) {
        val name = drawable?.trim()?.takeIf { it.isNotEmpty() } ?: return
        order += name
        val key = component?.let(ComponentKey::parse) ?: return
        mappings.getOrPut(key) { mutableListOf() }.let { if (name !in it) it += name }
        componentsByDrawable.getOrPut(name) { mutableListOf() }.let { if (key !in it) it += key }
    }

    /** `<item drawable="name"/>` from drawable.xml. */
    fun onDrawableItem(drawable: String?) {
        drawable?.trim()?.takeIf { it.isNotEmpty() }?.let { order += it }
    }

    /** [exists] filters out names that don't resolve to a drawable in the pack. */
    fun build(exists: (String) -> Boolean = { true }): IconPackIndex {
        val valid = order.filter(exists)
        val validSet = valid.toHashSet()
        val entries = valid.map { IconEntry(it, labelFor(it), componentsByDrawable[it].orEmpty()) }
        val filtered = mappings
            .mapValues { (_, names) -> names.filter { it in validSet } }
            .filterValues { it.isNotEmpty() }
        return IconPackIndex(packPackage, entries, filtered)
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")

        /** `whatsapp_alt_03` -> "Whatsapp alt 03". */
        fun labelFor(drawableName: String): String =
            WHITESPACE.replace(drawableName.replace('_', ' '), " ").trim()
                .replaceFirstChar { it.uppercaseChar() }
    }
}

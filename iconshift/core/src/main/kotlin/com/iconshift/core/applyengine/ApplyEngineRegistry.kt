package com.iconshift.core.applyengine

/**
 * Picks the engine to use. The UI talks to this, never to a specific engine, so adding a
 * manufacturer/launcher is a matter of registering another [IconApplyEngine].
 */
class ApplyEngineRegistry(private val engines: List<IconApplyEngine>) {

    data class Ranked(val engine: IconApplyEngine, val compatibility: CompatibilityResult)

    val all: List<IconApplyEngine> get() = engines

    fun byId(id: String): IconApplyEngine? = engines.firstOrNull { it.id == id }

    /** Every engine with its compatibility, best first. */
    suspend fun rank(): List<Ranked> = engines
        .map { Ranked(it, safeCheck(it)) }
        .sortedWith(compareBy<Ranked> { it.compatibility.level.ordinal }.thenBy { it.compatibility.missingRequirements.size })

    /** The engine "Automatic" mode would use: best usable one, else an [UnsupportedApplyEngine] explaining why. */
    suspend fun automatic(): IconApplyEngine = automatic(rank())

    /** Same as [automatic], reusing a ranking the caller already has (compatibility checks can be slow). */
    fun automatic(ranked: List<Ranked>): IconApplyEngine {
        ranked.firstOrNull { it.compatibility.isUsable }?.let { return it.engine }
        val why = ranked.joinToString("; ") { r ->
            "${r.engine.displayName}: " + (r.compatibility.reasons + r.compatibility.missingRequirements.map { it.description })
                .joinToString(", ")
        }.ifEmpty { "No apply engines are registered." }
        return UnsupportedApplyEngine(why)
    }

    private suspend fun safeCheck(engine: IconApplyEngine): CompatibilityResult = try {
        engine.checkCompatibility()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        CompatibilityResult(engine.id, SupportLevel.Unsupported, listOf("Compatibility check failed: ${e.message}"))
    }
}

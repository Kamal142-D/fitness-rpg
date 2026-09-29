package com.iconshift.core.applyengine

/**
 * Runs an engine operation and then verifies it. This is the only producer of
 * [ApplyResult.Verified]: an engine's own "it worked" is downgraded to
 * [ApplyResult.AppliedUnverified] unless [IconApplyEngine.verify] confirms the expected state.
 */
object ApplyPipeline {

    suspend fun apply(
        engine: IconApplyEngine,
        target: AppTarget,
        icon: IconSource,
        onStage: (ApplyStage) -> Unit = {},
    ): ApplyResult {
        val compat = engine.checkCompatibility()
        gate(compat)?.let { return it }
        val raw = runCatchingEngine { engine.apply(target, icon, onStage) }
        return confirm(engine, target, raw, onStage, expectCustom = true)
    }

    suspend fun restore(
        engine: IconApplyEngine,
        target: AppTarget,
        onStage: (ApplyStage) -> Unit = {},
    ): ApplyResult {
        val compat = engine.checkCompatibility()
        gate(compat)?.let { return it }
        val raw = runCatchingEngine { engine.restore(target, onStage) }
        return confirm(engine, target, raw, onStage, expectCustom = false)
    }

    private fun gate(compat: CompatibilityResult): ApplyResult? = when {
        compat.missingRequirements.isNotEmpty() -> ApplyResult.NeedsSetup(
            compat.missingRequirements,
            "Additional permission is required.",
        )
        compat.level == SupportLevel.Unsupported -> ApplyResult.Unsupported(
            compat.reasons.joinToString("; ").ifEmpty { "This device is not supported by ${compat.engineId}." },
        )
        else -> null
    }

    private suspend fun confirm(
        engine: IconApplyEngine,
        target: AppTarget,
        raw: ApplyResult,
        onStage: (ApplyStage) -> Unit,
        expectCustom: Boolean,
    ): ApplyResult {
        // Only an engine-level "done, unverified" proceeds to verification. Failures pass through.
        // An engine returning Verified itself is treated as unverified: verification is ours.
        val claimed = when (raw) {
            is ApplyResult.AppliedUnverified -> raw
            is ApplyResult.Verified -> ApplyResult.AppliedUnverified(raw.method, raw.details)
            else -> return raw
        }
        onStage(ApplyStage.Verifying)
        val verification = try {
            engine.verify(target)
        } catch (e: Exception) {
            VerificationResult.Unknown("Verification threw: ${e.message}")
        }
        return when {
            expectCustom && verification is VerificationResult.CustomIconActive ->
                ApplyResult.Verified(claimed.method, "${claimed.details}\nVerified: ${verification.details}")
            !expectCustom && verification is VerificationResult.OriginalIconActive ->
                ApplyResult.Verified(claimed.method, "${claimed.details}\nVerified: ${verification.details}")
            else -> ApplyResult.AppliedUnverified(
                claimed.method,
                "${claimed.details}\nIcon could not be verified after ${if (expectCustom) "applying" else "restoring"}: " +
                    describe(verification),
            )
        }
    }

    private inline fun runCatchingEngine(block: () -> ApplyResult): ApplyResult = try {
        block()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        ApplyResult.Failed("Engine error: ${e.message ?: e::class.simpleName}", e.stackTraceToString().take(2000))
    }

    fun describe(v: VerificationResult): String = when (v) {
        is VerificationResult.CustomIconActive -> "custom icon active (${v.details})"
        is VerificationResult.OriginalIconActive -> "original icon active (${v.details})"
        is VerificationResult.Unknown -> "unknown (${v.reason})"
    }
}

package com.iconshift.core.applyengine

/** Selected when no real mechanism is available. Always reports why; never pretends to succeed. */
class UnsupportedApplyEngine(private val reason: String) : IconApplyEngine {
    override val id = "unsupported"
    override val displayName = "Unsupported"

    override suspend fun checkCompatibility() =
        CompatibilityResult(id, SupportLevel.Unsupported, listOf(reason))

    override suspend fun apply(target: AppTarget, icon: IconSource, onStage: (ApplyStage) -> Unit) =
        ApplyResult.Unsupported(reason)

    override suspend fun restore(target: AppTarget, onStage: (ApplyStage) -> Unit) =
        ApplyResult.Unsupported(reason)

    override suspend fun verify(target: AppTarget) = VerificationResult.Unknown(reason)
}

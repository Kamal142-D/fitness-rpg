package com.iconshift.core.applyengine

/**
 * A device/launcher-specific way of replacing an app's launcher icon.
 *
 * Engines report the raw outcome of their mechanism; they never claim success on their own.
 * [ApplyPipeline] runs [verify] afterwards and is the only place a [ApplyResult.Verified] is produced.
 * Engines must never fall back to pinned shortcuts.
 */
interface IconApplyEngine {
    /** Stable identifier, stored with assignments (`applyMethod`). */
    val id: String

    val displayName: String

    suspend fun checkCompatibility(): CompatibilityResult

    suspend fun apply(
        target: AppTarget,
        icon: IconSource,
        onStage: (ApplyStage) -> Unit = {},
    ): ApplyResult

    suspend fun restore(
        target: AppTarget,
        onStage: (ApplyStage) -> Unit = {},
    ): ApplyResult

    suspend fun verify(target: AppTarget): VerificationResult
}

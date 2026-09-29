package com.iconshift.core.applyengine

/** The app whose launcher icon should change. [activityName] is null for "the app's main entry". */
data class AppTarget(
    val packageName: String,
    val activityName: String?,
    val label: String,
)

/**
 * The replacement icon, already rendered to PNG. [origin] records where it came from
 * ("generated", "picker", later "pack:<packPackage>/<drawableName>").
 */
class IconSource(
    val pngBytes: ByteArray,
    val label: String,
    val origin: String,
) {
    init {
        require(Png.isPng(pngBytes)) { "IconSource must be PNG data" }
    }
}

/** Progress stages shown to the user. "Success" only ever follows [Verifying]. */
enum class ApplyStage { Preparing, Applying, RefreshingLauncher, Verifying }

enum class SupportLevel { Supported, Experimental, Limited, Unsupported }

/** Something the user must set up before an engine can run. */
enum class Requirement(val description: String) {
    ShizukuInstalled("Install Shizuku"),
    ShizukuRunning("Start Shizuku (Wireless debugging)"),
    ShizukuPermission("Grant IconShift access in Shizuku"),
}

data class CompatibilityResult(
    val engineId: String,
    val level: SupportLevel,
    val reasons: List<String>,
    val missingRequirements: List<Requirement> = emptyList(),
) {
    val isUsable: Boolean get() = level != SupportLevel.Unsupported && missingRequirements.isEmpty()
}

sealed interface ApplyResult {
    /** The engine made its change AND verification confirmed the expected icon state. */
    data class Verified(val method: String, val details: String) : ApplyResult

    /** The engine ran without error but the result could not be confirmed. Never shown as success. */
    data class AppliedUnverified(val method: String, val details: String) : ApplyResult

    data class NeedsSetup(val requirements: List<Requirement>, val message: String) : ApplyResult

    data class Unsupported(val reason: String) : ApplyResult

    data class Failed(val reason: String, val cause: String? = null) : ApplyResult
}

val ApplyResult.isVerifiedSuccess: Boolean get() = this is ApplyResult.Verified

sealed interface VerificationResult {
    /** The system now serves our custom icon for the target. */
    data class CustomIconActive(val details: String) : VerificationResult

    /** The system serves the original icon (no customization present). */
    data class OriginalIconActive(val details: String) : VerificationResult

    /** State could not be determined (missing access, unknown format, ...). */
    data class Unknown(val reason: String) : VerificationResult
}

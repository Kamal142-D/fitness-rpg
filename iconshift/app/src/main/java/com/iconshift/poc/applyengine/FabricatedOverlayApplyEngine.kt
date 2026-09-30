package com.iconshift.poc.applyengine

import android.content.Context
import android.os.Build
import android.os.Process
import com.iconshift.core.applyengine.AppTarget
import com.iconshift.core.applyengine.ApplyResult
import com.iconshift.core.applyengine.ApplyStage
import com.iconshift.core.applyengine.CompatibilityResult
import com.iconshift.core.applyengine.EngineStateStore
import com.iconshift.core.applyengine.IconApplyEngine
import com.iconshift.core.applyengine.IconSource
import com.iconshift.core.applyengine.SupportLevel
import com.iconshift.core.applyengine.VerificationResult
import com.iconshift.core.shell.Shell
import com.iconshift.poc.shizuku.ShizukuGate
import com.iconshift.poc.shizuku.withPipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Replaces the target app's own launcher-icon resource with a shell-owned fabricated overlay
 * (Android 14+ file-backed entries). The target APK is not modified; disabling the overlay
 * restores it. Whether HyperOS's launcher honours this over its theme icons is exactly what the
 * POC measures.
 */
class FabricatedOverlayApplyEngine(
    private val context: Context,
    private val gate: ShizukuGate,
    private val store: EngineStateStore,
    private val isMiuiFamily: () -> Boolean,
) : IconApplyEngine {

    override val id = ID
    override val displayName = "App resource overlay (Shizuku, Android 14+)"

    override suspend fun checkCompatibility(): CompatibilityResult {
        val missing = gate.missingRequirements()
        if (Build.VERSION.SDK_INT < 34) {
            return CompatibilityResult(id, SupportLevel.Unsupported, listOf("Needs Android 14+ (this is SDK ${Build.VERSION.SDK_INT})"), missing)
        }
        val reasons = buildList {
            add("Fabricated overlay owned by com.android.shell (unconfirmed on this device)")
            if (isMiuiFamily()) add("HyperOS may keep showing its theme icon for apps the theme covers")
        }
        return CompatibilityResult(id, SupportLevel.Experimental, reasons, missing)
    }

    override suspend fun apply(target: AppTarget, icon: IconSource, onStage: (ApplyStage) -> Unit): ApplyResult {
        onStage(ApplyStage.Preparing)
        if (!Shell.isValidPackage(target.packageName)) return ApplyResult.Failed("Invalid package name")
        val service = gate.service() ?: return ApplyResult.Failed(HELPER_UNAVAILABLE)
        val resources = withContext(Dispatchers.IO) {
            runCatching { AppIcons.iconResourceNames(context, target) }.getOrDefault(emptyList())
        }
        if (resources.isEmpty()) return ApplyResult.Failed("Could not resolve ${target.packageName}'s icon resource")

        // Remember what the untouched icon renders like, unless our overlay is already active.
        val name = overlayName(target)
        if (overlayState(target) != OverlayState.Enabled) {
            AppIcons.renderHash(context, target)?.let { store.put(originalKey(target), it.encodeToByteArray()) }
        }

        onStage(ApplyStage.Applying)
        val error = withContext(Dispatchers.IO) {
            withPipe(icon.pngBytes) { pfd ->
                service.registerIconOverlay(name, target.packageName, resources.toTypedArray(), pfd, userId())
            }
        }
        if (error != null) return ApplyResult.Failed("OverlayManager rejected the overlay", error)

        onStage(ApplyStage.RefreshingLauncher)
        delay(SETTLE_MS) // Overlay change broadcast -> launcher reloads the package's icon.
        return ApplyResult.AppliedUnverified(id, "Overlay com.android.shell:$name on $resources")
    }

    override suspend fun restore(target: AppTarget, onStage: (ApplyStage) -> Unit): ApplyResult {
        onStage(ApplyStage.Preparing)
        if (!Shell.isValidPackage(target.packageName)) return ApplyResult.Failed("Invalid package name")
        val service = gate.service() ?: return ApplyResult.Failed(HELPER_UNAVAILABLE)
        if (overlayState(target) == OverlayState.Absent) {
            return ApplyResult.AppliedUnverified(id, "No IconShift overlay registered for ${target.packageName}")
        }
        onStage(ApplyStage.Applying)
        val error = withContext(Dispatchers.IO) { service.unregisterIconOverlay(overlayName(target), userId()) }
        if (error != null) return ApplyResult.Failed("Could not remove overlay", error)
        onStage(ApplyStage.RefreshingLauncher)
        delay(SETTLE_MS)
        return ApplyResult.AppliedUnverified(id, "Overlay com.android.shell:${overlayName(target)} removed")
    }

    override suspend fun verify(target: AppTarget): VerificationResult {
        val state = overlayState(target)
        if (state == OverlayState.Unknown) return VerificationResult.Unknown("could not query OverlayManager")
        val now = withContext(Dispatchers.IO) { AppIcons.renderHash(context, target) }
            ?: return VerificationResult.Unknown("could not render ${target.packageName}'s icon")
        val original = store.get(originalKey(target))?.decodeToString()
        return when {
            state == OverlayState.Enabled && original != null && now != original ->
                VerificationResult.CustomIconActive("overlay enabled and the app's icon resource now renders differently")
            state == OverlayState.Enabled ->
                VerificationResult.Unknown("overlay enabled but the icon resource still renders the original")
            original == null || now == original ->
                VerificationResult.OriginalIconActive("no active overlay; icon resource renders the original")
            else -> VerificationResult.Unknown("no active overlay but the icon differs from the recorded original")
        }
    }

    private enum class OverlayState { Enabled, Disabled, Absent, Unknown }

    private suspend fun overlayState(target: AppTarget): OverlayState {
        val shell = gate.shell() ?: return OverlayState.Unknown
        val r = shell.exec("cmd overlay list ${Shell.quote(target.packageName)}")
        if (!r.ok) return OverlayState.Unknown
        val line = r.stdout.lines().firstOrNull { it.contains(overlayName(target)) } ?: return OverlayState.Absent
        return if (line.trim().startsWith("[x]")) OverlayState.Enabled else OverlayState.Disabled
    }

    private fun overlayName(target: AppTarget) = "iconshift_" + target.packageName.replace('.', '_')

    private fun originalKey(target: AppTarget) = "overlay.original.${target.packageName}"

    private fun userId(): Int = Process.myUid() / 100_000

    companion object {
        const val ID = "fabricated-overlay"
        private const val HELPER_UNAVAILABLE =
            "IconShift's Shizuku helper process didn't start (this method needs it; see the log)"
        private const val SETTLE_MS = 1_500L
    }
}

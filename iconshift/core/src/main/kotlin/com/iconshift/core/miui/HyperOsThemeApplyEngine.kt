package com.iconshift.core.miui

import com.iconshift.core.applyengine.AppTarget
import com.iconshift.core.applyengine.ApplyResult
import com.iconshift.core.applyengine.ApplyStage
import com.iconshift.core.applyengine.CompatibilityResult
import com.iconshift.core.applyengine.EngineStateStore
import com.iconshift.core.applyengine.IconApplyEngine
import com.iconshift.core.applyengine.IconSource
import com.iconshift.core.applyengine.Png
import com.iconshift.core.applyengine.SupportLevel
import com.iconshift.core.applyengine.VerificationResult
import com.iconshift.core.shell.Shell
import kotlinx.coroutines.delay

/** Device facts the HyperOS engine needs; detected on device by the app. */
data class HyperOsThemeEnvironment(
    val isMiuiFamily: Boolean,
    val osDescription: String,
    /** `ro.miui.ui.version.code`, written into the generated theme's description.xml. */
    val uiVersionCode: String,
    /** Exported ThemeManager activity that applies a local .mtz ("pkg/cls"), or null if none was found. */
    val applyComponent: String?,
    /** Permission guarding [applyComponent], or null when any app may start it. */
    val applyComponentPermission: String? = null,
)

/**
 * HyperOS/MIUI engine: replaces a single app's icon inside the currently applied theme `icons`
 * component and re-applies it through ThemeManager.
 *
 * 1. Read the applied icons zip (world-readable under /data/system/theme).
 * 2. Back up the target's current entries (once per customization), swap in our PNG, keep the rest.
 * 3. Package an icons-only .mtz and write it to shared storage.
 * 4. Ask ThemeManager to apply it via its exported apply activity, then wait for the icons file to change.
 *
 * System access goes through a [ThemeHost]: the Shizuku shell, or plain app APIs ([direct] = true),
 * which only works when ThemeManager's apply activity is open to every app.
 *
 * Launcher entries and the target app are never touched, so the real app still launches.
 * This is EXPERIMENTAL until confirmed on a physical HyperOS 3 device.
 */
class HyperOsThemeApplyEngine(
    private val host: ThemeHost,
    private val environment: suspend () -> HyperOsThemeEnvironment,
    private val store: EngineStateStore,
    private val iconsPaths: List<String> = DEFAULT_ICONS_PATHS,
    /** Read-only base used when no icons component is applied (stock theme), so other apps keep their icons. */
    private val fallbackBasePaths: List<String> = DEFAULT_FALLBACK_BASE_PATHS,
    private val pollAttempts: Int = 30,
    private val pollIntervalMs: Long = 1_000,
    private val clock: () -> Long = System::currentTimeMillis,
    /** True when [host] has no special privileges (normal app APIs only). */
    private val direct: Boolean = false,
    override val id: String = if (direct) DIRECT_ID else ID,
    override val displayName: String = if (direct) "HyperOS theme icons (no Shizuku)" else "HyperOS theme icons (Shizuku)",
    /** Non-null when a previous attempt showed this host can't open ThemeManager (then the engine is Unsupported). */
    private val blockedReason: suspend () -> String? = { null },
    /** Called when ThemeManager refused to open, so the block can be remembered. */
    private val onLaunchBlocked: suspend (String) -> Unit = {},
) : IconApplyEngine {

    override suspend fun checkCompatibility(): CompatibilityResult {
        val env = environment()
        val missing = host.missingRequirements()
        if (!env.isMiuiFamily) {
            return CompatibilityResult(id, SupportLevel.Unsupported, listOf("Not a MIUI/HyperOS device (${env.osDescription})"), missing)
        }
        val component = env.applyComponent
        if (component == null || !Shell.isValidComponent(component)) {
            return CompatibilityResult(
                id,
                SupportLevel.Unsupported,
                listOf("ThemeManager does not expose an apply entry point on this build"),
                missing,
            )
        }
        blockedReason()?.let { reason ->
            return CompatibilityResult(id, SupportLevel.Unsupported, listOf(reason), missing)
        }
        if (direct && env.applyComponentPermission != null) {
            return CompatibilityResult(
                id,
                SupportLevel.Unsupported,
                listOf("ThemeManager's apply screen requires ${env.applyComponentPermission}, so a normal app can't open it"),
                missing,
            )
        }
        val reasons = mutableListOf("${env.osDescription}: theme icons via $component (unconfirmed on this device)")
        var level = SupportLevel.Experimental
        // Only probed in direct mode: it's a cheap local file check there, while with the shell it
        // would have to start the Shizuku service just to render the engine list.
        if (direct && missing.isEmpty() && (iconsPaths + fallbackBasePaths).none { host.fileStamp(it) != null }) {
            if (direct) {
                // Without a readable base, applying would drop every other app's theme icon. Don't risk it.
                return CompatibilityResult(
                    id,
                    SupportLevel.Unsupported,
                    listOf("Current theme icons can't be read without Shizuku; applying could reset other apps' icons"),
                    missing,
                )
            }
            reasons += "Current theme icons can't be read here; other apps' theme icons may be reset"
            level = SupportLevel.Limited
        }
        return CompatibilityResult(id, level, reasons, missing)
    }

    override suspend fun apply(target: AppTarget, icon: IconSource, onStage: (ApplyStage) -> Unit): ApplyResult {
        onStage(ApplyStage.Preparing)
        val pkg = target.packageName
        if (!Shell.isValidPackage(pkg)) return ApplyResult.Failed("Invalid package name: $pkg")
        if (!host.available()) return ApplyResult.Failed("System access unavailable")
        val env = environment()
        val component = env.applyComponent ?: return ApplyResult.Unsupported("No ThemeManager apply entry point")

        val located = locateIcons()
        val baseSource = located ?: locate(fallbackBasePaths)
        val base = baseSource?.second
        val entries = base?.let { MiuiIconsZip.read(it) } ?: LinkedHashMap()
        val currentForPkg = MiuiIconsZip.entriesFor(entries, pkg)

        // Refresh the backup unless the target currently shows OUR icon (then the backup is still the original).
        val previousHash = store.get(hashKey(pkg))?.decodeToString()
        val currentlyOurs = previousHash != null && currentForPkg.values.any { Png.sha256(it) == previousHash }
        if (!currentlyOurs) store.put(backupKey(pkg), MiuiIconsZip.write(currentForPkg))

        val iconDir = MiuiIconsZip.detectIconDir(entries)
        val entryPath = MiuiIconsZip.entryName(pkg, iconDir)
        val newIcons = MiuiIconsZip.rewrite(base, setOf(pkg), mapOf(entryPath to icon.pngBytes))
        val hash = Png.sha256(icon.pngBytes)
        store.put(hashKey(pkg), hash.encodeToByteArray())

        val details = buildString {
            appendLine("Base icons: ${baseSource?.first ?: "none found (creating new icons component)"} (${entries.size} entries)")
            appendLine("Target entries before: ${currentForPkg.keys.ifEmpty { setOf("<none: app was not themed>") }}")
            appendLine("Override: $entryPath sha256=${hash.take(16)}")
        }
        return applyIconsZip(env, component, newIcons, located, details, onStage)
    }

    override suspend fun restore(target: AppTarget, onStage: (ApplyStage) -> Unit): ApplyResult {
        onStage(ApplyStage.Preparing)
        val pkg = target.packageName
        if (!Shell.isValidPackage(pkg)) return ApplyResult.Failed("Invalid package name: $pkg")
        if (!host.available()) return ApplyResult.Failed("System access unavailable")
        val env = environment()
        val component = env.applyComponent ?: return ApplyResult.Unsupported("No ThemeManager apply entry point")

        val backupZip = store.get(backupKey(pkg))
            ?: return ApplyResult.AppliedUnverified(id, "No IconShift customization recorded for $pkg; nothing to restore.")
        val backup = MiuiIconsZip.read(backupZip)
        val located = locateIcons()
            ?: return ApplyResult.Failed("Current theme icons could not be read; cannot restore safely.")
        val entries = MiuiIconsZip.read(located.second)
        val expected = store.get(hashKey(pkg))?.decodeToString()
        val customPresent = expected != null &&
            MiuiIconsZip.entriesFor(entries, pkg).values.any { Png.sha256(it) == expected }
        if (!customPresent) {
            return ApplyResult.AppliedUnverified(id, "Custom icon for $pkg is not present in the applied theme; nothing to restore.")
        }
        val newIcons = MiuiIconsZip.rewrite(located.second, setOf(pkg), backup)
        val details = "Restoring ${backup.size} original entr${if (backup.size == 1) "y" else "ies"} for $pkg" +
            (if (backup.isEmpty()) " (app was not themed; removing override)" else "") + "\n"
        return applyIconsZip(env, component, newIcons, located, details, onStage)
    }

    override suspend fun verify(target: AppTarget): VerificationResult {
        val pkg = target.packageName
        if (!host.available()) return VerificationResult.Unknown("System access unavailable")
        val located = locateIcons() ?: return VerificationResult.Unknown("Applied theme icons not found at $iconsPaths")
        val current = MiuiIconsZip.entriesFor(MiuiIconsZip.read(located.second), pkg)
        val currentHashes = current.values.map(Png::sha256).toSet()
        val expected = store.get(hashKey(pkg))?.decodeToString()
        if (expected != null && expected in currentHashes) {
            return VerificationResult.CustomIconActive("${located.first} contains IconShift icon for $pkg (sha256 ${expected.take(16)})")
        }
        val backup = store.get(backupKey(pkg))?.let(MiuiIconsZip::read)
        if (backup == null) {
            return VerificationResult.OriginalIconActive("no IconShift override for $pkg in ${located.first}")
        }
        val backupHashes = backup.values.map(Png::sha256).toSet()
        return if (backupHashes == currentHashes) {
            VerificationResult.OriginalIconActive("${located.first} entries for $pkg match the saved original")
        } else {
            VerificationResult.Unknown("entries for $pkg match neither the IconShift icon nor the saved original (theme changed?)")
        }
    }

    private suspend fun applyIconsZip(
        env: HyperOsThemeEnvironment,
        component: String,
        iconsZip: ByteArray,
        before: Pair<String, ByteArray>?,
        details: String,
        onStage: (ApplyStage) -> Unit,
    ): ApplyResult {
        val mtz = MtzPackager.build(MtzPackager.Description(uiVersion = env.uiVersionCode), iconsZip)
        val fileName = "iconshift_${clock()}.mtz"

        onStage(ApplyStage.Applying)
        val mtzPath = try {
            host.writeThemePackage(fileName, mtz)
        } catch (e: Exception) {
            return ApplyResult.Failed("Could not write theme package $fileName", e.message)
        }
        val stampBefore = before?.let { host.fileStamp(it.first) }
        val launch = host.launchApply(component, mtzPath)
        if (!launch.ok) {
            onLaunchBlocked(launch.detail)
            return if (direct) {
                ApplyResult.Unsupported("$BLOCKED_DIRECT_MESSAGE\n${launch.detail}")
            } else {
                ApplyResult.Failed("ThemeManager refused the apply request", launch.detail)
            }
        }

        onStage(ApplyStage.RefreshingLauncher)
        val changed = waitForIconsChange(before, stampBefore)
        val log = details + "Theme package: $mtzPath (${mtz.size} bytes)\n${launch.detail}\n" +
            if (changed != null) "Applied icons updated: $changed" else "Applied icons did not change within ${pollAttempts * pollIntervalMs / 1000}s"
        return ApplyResult.AppliedUnverified(id, log)
    }

    /** Polls until the applied icons file appears or its mtime/size changes. Returns the path, or null on timeout. */
    private suspend fun waitForIconsChange(
        before: Pair<String, ByteArray>?,
        stampBefore: String?,
    ): String? {
        repeat(pollAttempts) {
            delay(pollIntervalMs)
            for (path in iconsPaths) {
                val now = host.fileStamp(path) ?: continue
                if (before == null || path != before.first || now != stampBefore) return path
            }
        }
        return null
    }

    private suspend fun locateIcons(): Pair<String, ByteArray>? = locate(iconsPaths)

    private suspend fun locate(paths: List<String>): Pair<String, ByteArray>? {
        for (path in paths) {
            val bytes = host.readFile(path) ?: continue
            if (bytes.isNotEmpty()) return path to bytes
        }
        return null
    }

    companion object {
        const val ID = "hyperos-theme"
        const val DIRECT_ID = "hyperos-theme-direct"
        const val BLOCKED_DIRECT_MESSAGE = "HyperOS blocks apps from opening Themes directly; Shizuku is needed on this phone"
        val DEFAULT_ICONS_PATHS = listOf("/data/system/theme/icons")
        const val DEFAULT_OUTPUT_DIR = "/sdcard/Download/IconShift"
        val DEFAULT_FALLBACK_BASE_PATHS = listOf("/system/media/theme/default/icons")

        private fun backupKey(pkg: String) = "hyperos.backup.$pkg"
        private fun hashKey(pkg: String) = "hyperos.hash.$pkg"
    }
}

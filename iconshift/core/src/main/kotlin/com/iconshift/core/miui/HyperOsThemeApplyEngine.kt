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
import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.Shell
import com.iconshift.core.shell.ShellAccess
import kotlinx.coroutines.delay

/** Device facts the HyperOS engine needs; detected on device by the app. */
data class HyperOsThemeEnvironment(
    val isMiuiFamily: Boolean,
    val osDescription: String,
    /** `ro.miui.ui.version.code`, written into the generated theme's description.xml. */
    val uiVersionCode: String,
    /** Exported ThemeManager activity that applies a local .mtz ("pkg/cls"), or null if none was found. */
    val applyComponent: String?,
)

/**
 * HyperOS/MIUI engine: replaces a single app's icon inside the currently applied theme `icons`
 * component and re-applies it through ThemeManager.
 *
 * 1. Read the applied icons zip (world-readable under /data/system/theme).
 * 2. Back up the target's current entries (once per customization), swap in our PNG, keep the rest.
 * 3. Package an icons-only .mtz, write it to shared storage via the shell.
 * 4. Ask ThemeManager to apply it via its exported apply activity, then wait for the icons file to change.
 *
 * Launcher entries and the target app are never touched, so the real app still launches.
 * This is EXPERIMENTAL until confirmed on a physical HyperOS 3 device.
 */
class HyperOsThemeApplyEngine(
    private val access: ShellAccess,
    private val environment: suspend () -> HyperOsThemeEnvironment,
    private val store: EngineStateStore,
    private val iconsPaths: List<String> = DEFAULT_ICONS_PATHS,
    private val outputDir: String = DEFAULT_OUTPUT_DIR,
    private val pollAttempts: Int = 30,
    private val pollIntervalMs: Long = 1_000,
    private val clock: () -> Long = System::currentTimeMillis,
) : IconApplyEngine {

    override val id = ID
    override val displayName = "HyperOS theme icons (Shizuku)"

    override suspend fun checkCompatibility(): CompatibilityResult {
        val env = environment()
        val missing = access.missingRequirements()
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
        return CompatibilityResult(
            id,
            SupportLevel.Experimental,
            listOf("${env.osDescription}: theme icons via $component (unconfirmed on this device)"),
            missing,
        )
    }

    override suspend fun apply(target: AppTarget, icon: IconSource, onStage: (ApplyStage) -> Unit): ApplyResult {
        onStage(ApplyStage.Preparing)
        val pkg = target.packageName
        if (!Shell.isValidPackage(pkg)) return ApplyResult.Failed("Invalid package name: $pkg")
        val shell = access.shell() ?: return ApplyResult.Failed("Privileged shell unavailable")
        val env = environment()
        val component = env.applyComponent ?: return ApplyResult.Unsupported("No ThemeManager apply entry point")

        val located = locateIcons(shell)
        val base = located?.second
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
            appendLine("Base icons: ${located?.first ?: "none applied (creating new icons component)"} (${entries.size} entries)")
            appendLine("Target entries before: ${currentForPkg.keys.ifEmpty { setOf("<none: app was not themed>") }}")
            appendLine("Override: $entryPath sha256=${hash.take(16)}")
        }
        return applyIconsZip(shell, env, component, newIcons, located, details, onStage)
    }

    override suspend fun restore(target: AppTarget, onStage: (ApplyStage) -> Unit): ApplyResult {
        onStage(ApplyStage.Preparing)
        val pkg = target.packageName
        if (!Shell.isValidPackage(pkg)) return ApplyResult.Failed("Invalid package name: $pkg")
        val shell = access.shell() ?: return ApplyResult.Failed("Privileged shell unavailable")
        val env = environment()
        val component = env.applyComponent ?: return ApplyResult.Unsupported("No ThemeManager apply entry point")

        val backupZip = store.get(backupKey(pkg))
            ?: return ApplyResult.AppliedUnverified(ID, "No IconShift customization recorded for $pkg; nothing to restore.")
        val backup = MiuiIconsZip.read(backupZip)
        val located = locateIcons(shell)
            ?: return ApplyResult.Failed("Current theme icons could not be read; cannot restore safely.")
        val entries = MiuiIconsZip.read(located.second)
        val expected = store.get(hashKey(pkg))?.decodeToString()
        val customPresent = expected != null &&
            MiuiIconsZip.entriesFor(entries, pkg).values.any { Png.sha256(it) == expected }
        if (!customPresent) {
            return ApplyResult.AppliedUnverified(ID, "Custom icon for $pkg is not present in the applied theme; nothing to restore.")
        }
        val newIcons = MiuiIconsZip.rewrite(located.second, setOf(pkg), backup)
        val details = "Restoring ${backup.size} original entr${if (backup.size == 1) "y" else "ies"} for $pkg" +
            (if (backup.isEmpty()) " (app was not themed; removing override)" else "") + "\n"
        return applyIconsZip(shell, env, component, newIcons, located, details, onStage)
    }

    override suspend fun verify(target: AppTarget): VerificationResult {
        val pkg = target.packageName
        val shell = access.shell() ?: return VerificationResult.Unknown("Privileged shell unavailable")
        val located = locateIcons(shell) ?: return VerificationResult.Unknown("Applied theme icons not found at $iconsPaths")
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
        shell: PrivilegedShell,
        env: HyperOsThemeEnvironment,
        component: String,
        iconsZip: ByteArray,
        before: Pair<String, ByteArray>?,
        details: String,
        onStage: (ApplyStage) -> Unit,
    ): ApplyResult {
        val mtz = MtzPackager.build(MtzPackager.Description(uiVersion = env.uiVersionCode), iconsZip)
        val mtzPath = "$outputDir/iconshift_${clock()}.mtz"

        onStage(ApplyStage.Applying)
        try {
            shell.writeFile(mtzPath, mtz)
        } catch (e: Exception) {
            return ApplyResult.Failed("Could not write theme package to $mtzPath", e.message)
        }
        val statBefore = before?.let { stat(shell, it.first) }
        val cmd = "am start -W -n ${Shell.quote(component)} " +
            "-e theme_file_path ${Shell.quote(mtzPath)} -e api_called_from test"
        val result = shell.exec(cmd)
        if (!result.ok || result.stdout.contains("Error") || result.stderr.contains("Error")) {
            return ApplyResult.Failed("ThemeManager refused the apply request", result.summary())
        }

        onStage(ApplyStage.RefreshingLauncher)
        val changed = waitForIconsChange(shell, before, statBefore)
        val log = details + "Theme package: $mtzPath (${mtz.size} bytes)\nam: ${result.summary()}\n" +
            if (changed != null) "Applied icons updated: $changed" else "Applied icons did not change within ${pollAttempts * pollIntervalMs / 1000}s"
        return ApplyResult.AppliedUnverified(ID, log)
    }

    /** Polls until the applied icons file appears or its mtime/size changes. Returns the path, or null on timeout. */
    private suspend fun waitForIconsChange(
        shell: PrivilegedShell,
        before: Pair<String, ByteArray>?,
        statBefore: String?,
    ): String? {
        repeat(pollAttempts) {
            delay(pollIntervalMs)
            for (path in iconsPaths) {
                val now = stat(shell, path) ?: continue
                if (before == null || path != before.first || now != statBefore) return path
            }
        }
        return null
    }

    private suspend fun stat(shell: PrivilegedShell, path: String): String? {
        val r = shell.exec("stat -c '%Y:%s' ${Shell.quote(path)}")
        return if (r.ok) r.stdout.trim().ifEmpty { null } else null
    }

    private suspend fun locateIcons(shell: PrivilegedShell): Pair<String, ByteArray>? {
        for (path in iconsPaths) {
            val bytes = shell.readFile(path) ?: continue
            if (bytes.isNotEmpty()) return path to bytes
        }
        return null
    }

    companion object {
        const val ID = "hyperos-theme"
        val DEFAULT_ICONS_PATHS = listOf("/data/system/theme/icons")
        const val DEFAULT_OUTPUT_DIR = "/sdcard/Download/IconShift"

        private fun backupKey(pkg: String) = "hyperos.backup.$pkg"
        private fun hashKey(pkg: String) = "hyperos.hash.$pkg"
    }
}

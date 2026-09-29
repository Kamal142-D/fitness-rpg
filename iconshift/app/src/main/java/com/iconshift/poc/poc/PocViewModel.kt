package com.iconshift.poc.poc

import android.app.Application
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iconshift.core.applyengine.AppTarget
import com.iconshift.core.applyengine.ApplyPipeline
import com.iconshift.core.applyengine.ApplyResult
import com.iconshift.core.applyengine.ApplyStage
import com.iconshift.core.applyengine.CompatibilityResult
import com.iconshift.core.applyengine.IconApplyEngine
import com.iconshift.core.applyengine.IconSource
import com.iconshift.core.applyengine.Png
import com.iconshift.core.applyengine.VerificationResult
import com.iconshift.core.miui.HyperOsThemeApplyEngine
import com.iconshift.core.miui.MiuiIconsZip
import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.Shell
import com.iconshift.poc.BuildConfig
import com.iconshift.poc.IconShiftApp
import com.iconshift.poc.applyengine.AppIcons
import com.iconshift.poc.device.ThemeManagerProbe
import com.iconshift.poc.shizuku.ShizukuGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Manual checks from the spec's Phase 1 list; only a human looking at the launcher can answer these. */
enum class Check(val label: String) {
    HomeScreen("Icon changed on the home screen"),
    Dock("Icon changed in the dock"),
    NoFrame("No white border / frame around the icon"),
    LaunchesApp("Tapping it opens the real app"),
    NoDuplicate("No duplicate icon or shortcut created"),
    AppIntact("Original app still works normally"),
    SurvivesReboot("Custom icon still there after reboot"),
    Restore("Restore brings back the original icon"),
}

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap?,
) {
    val target get() = AppTarget(packageName, activityName, label)
}

data class EngineUi(
    val id: String,
    val name: String,
    val compatibility: CompatibilityResult? = null,
    val busy: Boolean = false,
    val stage: ApplyStage? = null,
    val lastResult: String? = null,
    val checks: Map<Check, Boolean> = emptyMap(),
)

data class PocState(
    val deviceLines: List<String> = emptyList(),
    val themeLines: List<String> = emptyList(),
    val shizuku: ShizukuGate.Status = ShizukuGate.Status.NotInstalled,
    val shizukuDetail: String = "",
    val apps: List<AppEntry> = emptyList(),
    val target: AppEntry? = null,
    val targetResourceIcon: ImageBitmap? = null,
    val icon: IconSource? = null,
    val iconPreview: ImageBitmap? = null,
    val engines: List<EngineUi> = emptyList(),
    val automaticEngine: String = "",
    val log: List<String> = emptyList(),
    val diagnosticsBusy: Boolean = false,
)

class PocViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as IconShiftApp).container
    private val _state = MutableStateFlow(PocState())
    val state: StateFlow<PocState> = _state.asStateFlow()

    init {
        setIcon(TestIcons.generated())
        // Shizuku starting or granting permission changes which engines are usable.
        viewModelScope.launch(Dispatchers.IO) {
            container.shizuku.status.collect {
                refreshShizuku()
                refreshEngines()
            }
        }
        refreshAll()
    }

    // --- Probing -------------------------------------------------------------------------------

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            container.refreshProbes()
            _state.update {
                it.copy(
                    deviceLines = container.device.lines(),
                    themeLines = container.themeManager.lines(),
                )
            }
            refreshShizuku()
            if (_state.value.apps.isEmpty()) loadApps()
            refreshEngines()
            refreshTargetPreview()
        }
    }

    private fun refreshShizuku() {
        container.shizuku.refresh()
        val gate = container.shizuku
        val uid = gate.shizukuUid()
        val detail = when (uid) {
            null -> ""
            0 -> "running as root (uid 0)"
            2000 -> "running as shell (uid 2000)"
            else -> "running as uid $uid"
        } + (gate.shizukuVersion()?.let { " · API $it" } ?: "")
        _state.update { it.copy(shizuku = gate.status.value, shizukuDetail = detail) }
    }

    fun requestShizukuPermission() = container.shizuku.requestPermission()

    private suspend fun refreshEngines() {
        val ranked = container.engines.rank()
        val previous = _state.value.engines.associateBy { it.id }
        val automatic = container.engines.automatic()
        val automaticText = if (automatic.id == "unsupported") {
            "None usable: " + automatic.checkCompatibility().reasons.joinToString("; ")
        } else {
            automatic.displayName
        }
        _state.update { s ->
            s.copy(
                engines = container.engines.all.map { e ->
                    val compat = ranked.firstOrNull { it.engine.id == e.id }?.compatibility
                    (previous[e.id] ?: EngineUi(e.id, e.displayName)).copy(compatibility = compat)
                },
                automaticEngine = automaticText,
            )
        }
    }

    private fun loadApps() {
        val ctx = getApplication<Application>()
        val la = ctx.getSystemService(LauncherApps::class.java)
        val apps = la.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != ctx.packageName }
            .map {
                AppEntry(
                    label = it.label.toString(),
                    packageName = it.componentName.packageName,
                    activityName = it.componentName.className,
                    icon = runCatching { it.getIcon(0).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
        val default = apps.firstOrNull { it.packageName == DEFAULT_TARGET } ?: apps.firstOrNull()
        _state.update { it.copy(apps = apps, target = it.target ?: default) }
    }

    fun selectTarget(entry: AppEntry) {
        _state.update { it.copy(target = entry) }
        log("Target: ${entry.label} (${entry.packageName}/${entry.activityName})")
        viewModelScope.launch(Dispatchers.IO) { refreshTargetPreview() }
    }

    private fun refreshTargetPreview() {
        val target = _state.value.target ?: return
        val bmp = AppIcons.render(getApplication(), target.target, 192)
        _state.update { it.copy(targetResourceIcon = bmp?.asImageBitmap()) }
    }

    // --- Icon ----------------------------------------------------------------------------------

    fun useGeneratedIcon() = setIcon(TestIcons.generated())

    fun usePickedImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                setIcon(TestIcons.fromUri(getApplication<Application>().contentResolver, uri))
            } catch (e: Exception) {
                log("Could not read picked image: ${e.message}")
            }
        }
    }

    private fun setIcon(icon: IconSource) {
        val bmp = BitmapFactory.decodeByteArray(icon.pngBytes, 0, icon.pngBytes.size)
        _state.update { it.copy(icon = icon, iconPreview = bmp?.asImageBitmap()) }
        log("Icon: ${icon.label} (${icon.origin}, ${icon.pngBytes.size} bytes, sha256 ${Png.sha256(icon.pngBytes).take(16)})")
    }

    // --- Engine operations ---------------------------------------------------------------------

    fun apply(engineId: String) = runEngine(engineId, "Apply") { engine, target, onStage ->
        val icon = _state.value.icon ?: return@runEngine ApplyResult.Failed("No icon selected")
        ApplyPipeline.apply(engine, target, icon, onStage)
    }

    fun restore(engineId: String) = runEngine(engineId, "Restore") { engine, target, onStage ->
        ApplyPipeline.restore(engine, target, onStage)
    }

    fun verify(engineId: String) {
        val engine = container.engines.byId(engineId) ?: return
        val target = _state.value.target?.target ?: return
        viewModelScope.launch(Dispatchers.IO) {
            updateEngine(engineId) { it.copy(busy = true, stage = ApplyStage.Verifying) }
            val v = try {
                engine.verify(target)
            } catch (e: Exception) {
                VerificationResult.Unknown("threw ${e.message}")
            }
            val text = "Verify: ${ApplyPipeline.describe(v)}"
            log("[${engine.displayName}] $text")
            updateEngine(engineId) { it.copy(busy = false, stage = null, lastResult = text) }
            refreshTargetPreview()
        }
    }

    private fun runEngine(
        engineId: String,
        action: String,
        block: suspend (IconApplyEngine, AppTarget, (ApplyStage) -> Unit) -> ApplyResult,
    ) {
        val engine = container.engines.byId(engineId) ?: return
        val target = _state.value.target?.target ?: run {
            log("Select a target app first")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            updateEngine(engineId) { it.copy(busy = true, stage = null, lastResult = null) }
            log("[${engine.displayName}] $action ${target.packageName} …")
            val result = block(engine, target) { stage ->
                log("[${engine.displayName}] $stage")
                updateEngine(engineId) { it.copy(stage = stage) }
            }
            val text = describe(action, result)
            log("[${engine.displayName}] $text")
            updateEngine(engineId) { it.copy(busy = false, stage = null, lastResult = text) }
            refreshEngines()
            refreshTargetPreview()
        }
    }

    fun setCheck(engineId: String, check: Check, passed: Boolean?) {
        updateEngine(engineId) { e ->
            e.copy(checks = if (passed == null) e.checks - check else e.checks + (check to passed))
        }
    }

    private fun updateEngine(id: String, change: (EngineUi) -> EngineUi) {
        _state.update { s -> s.copy(engines = s.engines.map { if (it.id == id) change(it) else it }) }
    }

    // --- Diagnostics ---------------------------------------------------------------------------

    fun restartLauncher() = diagnostic("Restart launcher") { shell ->
        val pkg = container.device.launcherPackage
        if (!Shell.isValidPackage(pkg)) return@diagnostic "Unknown launcher package: $pkg"
        shell.exec("am force-stop ${Shell.quote(pkg)}").summary()
    }

    fun inspectThemeIcons() = diagnostic("Inspect theme icons") { shell ->
        buildString {
            appendLine(shell.exec("ls -la /data/system/theme/ 2>&1; ls -la ${HyperOsThemeApplyEngine.DEFAULT_OUTPUT_DIR} 2>&1").stdout.trim())
            val path = HyperOsThemeApplyEngine.DEFAULT_ICONS_PATHS.first()
            val bytes = shell.readFile(path)
            if (bytes == null) {
                appendLine("$path: not readable/absent")
                return@buildString
            }
            val entries = runCatching { MiuiIconsZip.read(bytes) }.getOrElse {
                appendLine("$path: ${bytes.size} bytes, not a zip: ${it.message}")
                return@buildString
            }
            appendLine("$path: ${bytes.size} bytes, ${entries.size} entries, icon dir ${MiuiIconsZip.detectIconDir(entries)}")
            appendLine("Top-level: " + entries.keys.map { it.substringBefore('/') }.distinct().take(20).joinToString())
            _state.value.target?.let { t ->
                val mine = MiuiIconsZip.entriesFor(entries, t.packageName)
                appendLine("Entries for ${t.packageName}: " + mine.keys.ifEmpty { setOf("<none: not themed>") }.joinToString())
            }
            appendLine("Sample: " + entries.keys.take(12).joinToString())
        }
    }

    fun dumpOverlays() = diagnostic("Overlay state") { shell ->
        val pkg = _state.value.target?.packageName ?: return@diagnostic "No target"
        shell.exec("cmd overlay list ${Shell.quote(pkg)} 2>&1").let { it.stdout.trim().ifEmpty { it.summary() } }
    }

    fun dumpThemeManager() = diagnostic("ThemeManager components") { shell ->
        shell.exec("dumpsys package ${ThemeManagerProbe.PACKAGE} | grep -iE 'Apply|Import|mtz' | head -40").stdout.trim()
            .ifEmpty { "no matches" }
    }

    private fun diagnostic(name: String, block: suspend (PrivilegedShell) -> String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(diagnosticsBusy = true) }
            val shell = container.shizuku.shell()
            val out = if (shell == null) "Shizuku not ready" else try {
                block(shell)
            } catch (e: Exception) {
                "failed: $e"
            }
            log("[$name]\n$out")
            _state.update { it.copy(diagnosticsBusy = false) }
        }
    }

    // --- Log & report --------------------------------------------------------------------------

    private fun log(line: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date())
        _state.update { it.copy(log = (it.log + "$stamp $line").takeLast(MAX_LOG)) }
    }

    fun buildReport(): String {
        val s = _state.value
        return buildString {
            appendLine("IconShift POC report ${BuildConfig.VERSION_NAME} — ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(Date())}")
            appendLine()
            appendLine("== Device")
            s.deviceLines.forEach { appendLine(it) }
            appendLine()
            appendLine("== ThemeManager")
            s.themeLines.forEach { appendLine(it) }
            appendLine()
            appendLine("== Shizuku: ${s.shizuku} ${s.shizukuDetail}")
            appendLine("== Automatic engine: ${s.automaticEngine}")
            appendLine("== Target: ${s.target?.let { "${it.label} ${it.packageName}/${it.activityName}" } ?: "-"}")
            appendLine("== Icon: ${s.icon?.let { "${it.label} (${it.origin}) sha256 ${Png.sha256(it.pngBytes).take(16)}" } ?: "-"}")
            for (e in s.engines) {
                appendLine()
                appendLine("== Engine ${e.name} [${e.id}]")
                e.compatibility?.let { c ->
                    appendLine("Compatibility: ${c.level}; ${c.reasons.joinToString("; ")}")
                    if (c.missingRequirements.isNotEmpty()) appendLine("Missing: ${c.missingRequirements.joinToString { it.description }}")
                }
                appendLine("Last result: ${e.lastResult ?: "-"}")
                for (check in Check.entries) {
                    val v = e.checks[check]
                    appendLine("  [${when (v) { true -> "PASS"; false -> "FAIL"; null -> " -- " }}] ${check.label}")
                }
            }
            appendLine()
            appendLine("== Log")
            s.log.forEach { appendLine(it) }
        }
    }

    companion object {
        private const val DEFAULT_TARGET = "com.whatsapp"
        private const val MAX_LOG = 400
    }
}

fun describe(action: String, r: ApplyResult): String = when (r) {
    is ApplyResult.Verified -> "$action VERIFIED (system level) — now check the launcher and tick the checklist.\n${r.details}"
    is ApplyResult.AppliedUnverified -> "$action ran but is NOT verified.\n${r.details}"
    is ApplyResult.NeedsSetup -> "$action needs setup: ${r.requirements.joinToString { it.description }}. ${r.message}"
    is ApplyResult.Unsupported -> "$action unsupported: ${r.reason}"
    is ApplyResult.Failed -> "$action FAILED: ${r.reason}" + (r.cause?.let { "\n$it" } ?: "")
}

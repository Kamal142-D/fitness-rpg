package com.iconshift.poc.poc

import android.app.Application
import android.content.pm.LauncherApps
import android.graphics.Bitmap
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
import com.iconshift.core.iconpack.IconEntry
import com.iconshift.core.iconpack.IconMatcher
import com.iconshift.core.iconpack.IconPackInfo
import com.iconshift.core.miui.HyperOsThemeApplyEngine
import com.iconshift.core.miui.MiuiIconsZip
import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.Shell
import com.iconshift.poc.BuildConfig
import com.iconshift.poc.IconShiftApp
import com.iconshift.poc.applyengine.AppIcons
import com.iconshift.poc.device.DeepProbe
import com.iconshift.poc.device.ThemeManagerProbe
import com.iconshift.poc.shizuku.ShizukuGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    /** Plain outcome of the last Apply/Restore, for the simple screen. */
    val lastAction: Action? = null,
    val lastOutcome: Outcome? = null,
    val lastReason: String? = null,
)

enum class Action { Apply, Restore }

enum class Outcome { Verified, Unverified, NeedsSetup, Unsupported, Failed }

private fun outcomeOf(r: ApplyResult): Pair<Outcome, String?> = when (r) {
    is ApplyResult.Verified -> Outcome.Verified to null
    is ApplyResult.AppliedUnverified -> Outcome.Unverified to null
    is ApplyResult.NeedsSetup -> Outcome.NeedsSetup to r.requirements.joinToString { it.description }
    is ApplyResult.Unsupported -> Outcome.Unsupported to r.reason
    is ApplyResult.Failed -> Outcome.Failed to r.reason
}

data class PackUi(
    val info: IconPackInfo,
    val icon: ImageBitmap?,
    val error: String? = null,
)

/** State of the "From icon pack" picker: pack list, then a searchable icon grid for one pack. */
data class PickerState(
    val open: Boolean = false,
    val loadingPacks: Boolean = false,
    val packs: List<PackUi> = emptyList(),
    val selected: PackUi? = null,
    val loadingIndex: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val recommended: List<IconEntry> = emptyList(),
    val results: List<IconEntry> = emptyList(),
    val total: Int = 0,
)

data class PocState(
    val deviceLines: List<String> = emptyList(),
    val themeLines: List<String> = emptyList(),
    val iconPackSummary: String = "",
    val shizuku: ShizukuGate.Status = ShizukuGate.Status.NotInstalled,
    val shizukuDetail: String = "",
    val apps: List<AppEntry> = emptyList(),
    val target: AppEntry? = null,
    val targetResourceIcon: ImageBitmap? = null,
    val icon: IconSource? = null,
    val iconPreview: ImageBitmap? = null,
    val engines: List<EngineUi> = emptyList(),
    val automaticEngine: String = "",
    /** Engine the one-tap "Apply icon" button uses; null when none is usable right now. */
    val automaticEngineId: String? = null,
    /** False until the first compatibility check finished (UI shows "Checking…" instead of "nothing works"). */
    val enginesChecked: Boolean = false,
    /** Engine of the last Apply/Restore; its result stays visible even if Automatic switches engines. */
    val lastRunEngineId: String? = null,
    val log: List<String> = emptyList(),
    val diagnosticsBusy: Boolean = false,
    val picker: PickerState = PickerState(),
)

class PocViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as IconShiftApp).container
    private val _state = MutableStateFlow(PocState())
    val state: StateFlow<PocState> = _state.asStateFlow()

    private var lastEngineSummary = ""

    init {
        // Show the methods right away; compatibility fills in when the checks finish.
        _state.update { s -> s.copy(engines = container.engines.all.map { EngineUi(it.id, it.displayName) }) }
        container.shizuku.onEvent = { log(it) }
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

    /** "Re-run checks": also forgets a remembered no-Shizuku block so it gets tried again. */
    fun rerunChecks() {
        container.clearDirectBlock()
        container.shizuku.resetBindCooldown()
        log("Checks re-run; the no-Shizuku method will be tried again")
        refreshAll()
    }

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
            refreshIconPackSummary()
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
        val automatic = container.engines.automatic(ranked)
        val summary = "Methods: " + ranked.joinToString { r ->
            "${r.engine.id}=${r.compatibility.level}" +
                if (r.compatibility.missingRequirements.isNotEmpty()) " (needs setup)" else ""
        } + " → automatic: ${automatic.id}"
        if (summary != lastEngineSummary) {
            lastEngineSummary = summary
            log(summary)
        }
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
                automaticEngineId = automatic.id.takeIf { it != "unsupported" },
                enginesChecked = true,
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

    // --- Icon packs -----------------------------------------------------------------------------

    private var searchJob: Job? = null
    private var countJob: Job? = null

    private fun refreshIconPackSummary() {
        val packs = container.iconPacks.detectPacks()
        val summary = "Icon packs: ${packs.size} detected" +
            if (packs.isNotEmpty()) " (${packs.joinToString { it.label }})" else ""
        _state.update { it.copy(iconPackSummary = summary) }
    }

    fun openIconPacks() {
        _state.update { it.copy(picker = PickerState(open = true, loadingPacks = true)) }
        countJob?.cancel()
        countJob = viewModelScope.launch(Dispatchers.IO) {
            val repo = container.iconPacks
            val packs = repo.detectPacks().map { PackUi(it, repo.appIcon(it.packageName)?.asImageBitmap()) }
            updatePicker { it.copy(loadingPacks = false, packs = packs) }
            // Count icons in the background. Loads are shared per pack, so opening a pack while
            // this runs waits for the same parse instead of starting a second one.
            for (pack in packs) {
                if (pack.info.iconCount != null) continue
                val updated = try {
                    pack.copy(info = pack.info.copy(iconCount = repo.loadIndex(pack.info.packageName).index.entries.size))
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    pack.copy(error = e.message ?: "This icon pack could not be read.")
                }
                updatePicker { p ->
                    p.copy(packs = p.packs.map { if (it.info.packageName == updated.info.packageName) updated else it })
                }
            }
        }
    }

    fun closeIconPacks() {
        searchJob?.cancel()
        countJob?.cancel()
        updatePicker { PickerState() }
    }

    fun backToPacks() {
        searchJob?.cancel()
        updatePicker { it.copy(selected = null, error = null, query = "", recommended = emptyList(), results = emptyList(), total = 0) }
    }

    fun selectPack(pack: PackUi) {
        updatePicker { it.copy(selected = pack, loadingIndex = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val load = container.iconPacks.loadIndex(pack.info.packageName)
                val index = load.index
                val target = _state.value.target
                val recommended = target?.let {
                    IconMatcher.recommend(index, it.packageName, it.activityName, it.label)
                }.orEmpty()
                // Honour anything typed while the pack was loading.
                val query = _state.value.picker.query
                val results = if (query.isBlank()) index.entries else IconMatcher.search(index, query)
                updatePicker {
                    if (it.selected?.info?.packageName != pack.info.packageName) {
                        it
                    } else {
                        it.copy(
                            loadingIndex = false,
                            recommended = recommended,
                            results = if (it.query == query) results else IconMatcher.search(index, it.query),
                            total = index.entries.size,
                        )
                    }
                }
                log(
                    "Icon pack ${pack.info.label}: ${index.entries.size} icons, ${index.componentToDrawables.size} mapped " +
                        "components, ${recommended.size} recommended — loaded in ${load.millis} ms (${load.source})",
                )
            } catch (e: Exception) {
                val msg = e.message ?: "This icon pack could not be read."
                updatePicker { it.copy(loadingIndex = false, error = msg) }
                log("Icon pack ${pack.info.packageName}: $msg")
            }
        }
    }

    fun searchIcons(query: String) {
        updatePicker { it.copy(query = query) }
        val pkg = _state.value.picker.selected?.info?.packageName ?: return
        val index = container.iconPacks.cachedIndex(pkg) ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce: while typing, only the last query runs (a search itself isn't cancellable mid-scan).
            delay(SEARCH_DEBOUNCE_MS)
            val results = IconMatcher.search(index, query)
            updatePicker { if (it.query == query) it.copy(results = results) else it }
        }
    }

    /** Thumbnail for a grid cell; call off the main thread. */
    fun packThumbnail(pkg: String, drawableName: String, sizePx: Int): Bitmap? =
        container.iconPacks.thumbnail(pkg, drawableName, sizePx)

    fun useIconFromPack(entry: IconEntry) {
        val pkg = _state.value.picker.selected?.info?.packageName ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                setIcon(container.iconPacks.iconSource(pkg, entry.drawableName))
                closeIconPacks()
                log("Icon selected — tap \"Apply icon\" to put it on the launcher")
            } catch (e: Exception) {
                val msg = e.message ?: "This icon is no longer available."
                updatePicker { it.copy(error = msg) }
                log("Could not load ${entry.drawableName}: $msg")
            }
        }
    }

    private fun updatePicker(change: (PickerState) -> PickerState) {
        _state.update { it.copy(picker = change(it.picker)) }
    }

    // --- Engine operations ---------------------------------------------------------------------

    fun apply(engineId: String) = runEngine(engineId, Action.Apply) { engine, target, onStage ->
        val icon = _state.value.icon ?: return@runEngine ApplyResult.Failed("No icon selected")
        ApplyPipeline.apply(engine, target, icon, onStage)
    }

    /** One-tap apply with the best usable engine (the same engine card shows progress and result). */
    fun applyAutomatic() {
        container.shizuku.resetBindCooldown()
        val id = _state.value.automaticEngineId ?: return log("No usable apply method yet: ${_state.value.automaticEngine}")
        apply(id)
    }

    fun restoreAutomatic() {
        val id = _state.value.automaticEngineId ?: return log("No usable apply method yet: ${_state.value.automaticEngine}")
        restore(id)
    }

    fun restore(engineId: String) = runEngine(engineId, Action.Restore) { engine, target, onStage ->
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
        action: Action,
        block: suspend (IconApplyEngine, AppTarget, (ApplyStage) -> Unit) -> ApplyResult,
    ) {
        val engine = container.engines.byId(engineId) ?: return
        val target = _state.value.target?.target ?: run {
            log("Select a target app first")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(lastRunEngineId = engineId) }
            updateEngine(engineId) {
                it.copy(busy = true, stage = null, lastResult = null, lastAction = action, lastOutcome = null, lastReason = null)
            }
            log("[${engine.displayName}] $action ${target.packageName} …")
            val result = block(engine, target) { stage ->
                log("[${engine.displayName}] $stage")
                updateEngine(engineId) { it.copy(stage = stage) }
            }
            val text = describe(action.name, result)
            log("[${engine.displayName}] $text")
            val (outcome, reason) = outcomeOf(result)
            updateEngine(engineId) {
                it.copy(busy = false, stage = null, lastResult = text, lastOutcome = outcome, lastReason = reason)
            }
            refreshEngines()
            refreshTargetPreview()
            // A theme apply that couldn't be confirmed: collect the facts right away for the report.
            if (engineId.startsWith("hyperos-theme") && result is ApplyResult.AppliedUnverified) runDeepProbe()
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

    /** Everything needed to see why ThemeManager ignored an apply (see [DeepProbe]). */
    fun deepProbe() {
        viewModelScope.launch(Dispatchers.IO) { runDeepProbe() }
    }

    private suspend fun runDeepProbe() {
        _state.update { it.copy(diagnosticsBusy = true) }
        log("[Deep probe] running…")
        val out = runCatching { DeepProbe.run(getApplication(), container.shizuku.shell()) }.getOrElse { "failed: $it" }
        log("[Deep probe]\n$out")
        _state.update { it.copy(diagnosticsBusy = false) }
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
            appendLine(s.iconPackSummary)
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
        private const val SEARCH_DEBOUNCE_MS = 150L
    }
}

fun describe(action: String, r: ApplyResult): String = when (r) {
    is ApplyResult.Verified -> "$action VERIFIED (system level) — now check the launcher and tick the checklist.\n${r.details}"
    is ApplyResult.AppliedUnverified -> "$action ran but is NOT verified.\n${r.details}"
    is ApplyResult.NeedsSetup -> "$action needs setup: ${r.requirements.joinToString { it.description }}. ${r.message}"
    is ApplyResult.Unsupported -> "$action unsupported: ${r.reason}"
    is ApplyResult.Failed -> "$action FAILED: ${r.reason}" + (r.cause?.let { "\n$it" } ?: "")
}

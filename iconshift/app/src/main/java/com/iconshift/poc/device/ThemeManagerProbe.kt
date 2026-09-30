package com.iconshift.poc.device

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * What Xiaomi's ThemeManager exposes on this build. The HyperOS engine only runs if an exported
 * apply entry point exists; the full candidate list goes into the report so a different entry
 * point can be wired in if HyperOS 3 renamed it.
 */
data class ThemeManagerProbe(
    val installed: Boolean,
    val version: String,
    val applyComponent: String?,
    /** Permission required to start [applyComponent]; null means any app may start it. */
    val applyPermission: String?,
    val candidates: List<String>,
    val exportedActivityCount: Int,
    val themeDirListing: List<String>,
) {
    fun lines(): List<String> = buildList {
        add("ThemeManager: ${if (installed) version else "not installed"}")
        add("Apply entry point: ${applyComponent ?: "NOT FOUND"}")
        if (applyComponent != null) add("Apply entry permission: ${applyPermission ?: "none (open to all apps)"}")
        add("Exported activities: $exportedActivityCount; theme-related candidates:")
        candidates.forEach { add("  - $it") }
        add("/data/system/theme (app-visible):")
        themeDirListing.ifEmpty { listOf("<not listable>") }.forEach { add("  $it") }
    }

    companion object {
        const val PACKAGE = "com.android.thememanager"
        private val KNOWN_APPLY_ACTIVITIES = listOf("ApplyThemeForScreenshot")
        private val CANDIDATE_WORDS = listOf("apply", "import", "local", "mtz")

        fun read(context: Context): ThemeManagerProbe {
            val pm = context.packageManager
            val info = runCatching {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(PACKAGE, PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS)
            }.getOrNull()
            val exportedInfos = info?.activities.orEmpty().filter { it.exported }
            val exported = exportedInfos.map { "${it.packageName}/${it.name}" }
            val applyInfo = exportedInfos.firstOrNull { a -> KNOWN_APPLY_ACTIVITIES.any { a.name.endsWith(".$it") } }
            val apply = applyInfo?.let { "${it.packageName}/${it.name}" }
            val candidates = exportedInfos.filter { a ->
                val simple = a.name.substringAfterLast('.').lowercase()
                CANDIDATE_WORDS.any { simple.contains(it) }
            }.map { a -> "${a.packageName}/${a.name}" + ((a.permission ?: a.applicationInfo?.permission)?.let { " [needs $it]" } ?: " [open]") }
            return ThemeManagerProbe(
                installed = info != null,
                version = info?.let { "${it.versionName} (${it.longVersionCode})" }.orEmpty(),
                applyComponent = apply,
                // An activity without its own permission inherits the application's.
                applyPermission = applyInfo?.let { it.permission ?: it.applicationInfo?.permission },
                candidates = candidates,
                exportedActivityCount = exported.size,
                themeDirListing = listThemeDir(),
            )
        }

        private fun listThemeDir(): List<String> = runCatching {
            File("/data/system/theme").listFiles().orEmpty()
                .sortedBy { it.name }
                .map { f -> "${f.name}${if (f.isDirectory) "/" else ""} ${if (f.isFile) f.length() else ""} r=${f.canRead()}".trim() }
        }.getOrDefault(emptyList())
    }
}

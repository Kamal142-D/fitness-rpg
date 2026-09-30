package com.iconshift.poc.device

import android.content.Context
import com.iconshift.core.dex.DexStrings
import com.iconshift.core.miui.MiuiIconsZip
import com.iconshift.core.shell.PrivilegedShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/**
 * Collects what's needed to understand why ThemeManager ignores an apply request, in one run:
 * the strings ThemeManager's code uses (extras/keys/paths), what it logged, and where HyperOS
 * keeps theme data. Read-only; nothing on the device is changed.
 */
object DeepProbe {

    suspend fun run(context: Context, shell: PrivilegedShell?): String = withContext(Dispatchers.IO) {
        buildString {
            section("ThemeManager code strings (apply-related)") { themeManagerStrings(context) }
            if (shell == null) {
                appendLine("== Shell checks skipped: Shizuku not ready")
                return@buildString
            }
            section("ThemeManager log (latest)") {
                shell.exec(
                    "logcat -d -t 3000 2>&1 | grep -iE 'thememanager|ApplyTheme|ThemeApply|mtz|theme_file|ThemeResources' | tail -80",
                ).stdout.trim().ifEmpty { "no matching log lines" }
            }
            section("Theme directories") {
                shell.exec(
                    "for d in /data/system/theme /data/system/theme/.runtime /data/system/theme_magic " +
                        "/system/media/theme/default /sdcard/MIUI/theme /sdcard/Download/IconShift; do " +
                        "echo \"# \$d\"; ls -la \"\$d\" 2>&1 | head -40; done",
                ).stdout.trim()
            }
            section("ApplyThemeForScreenshot in package manager") {
                shell.exec(
                    "dumpsys package ${ThemeManagerProbe.PACKAGE} 2>&1 | grep -iA6 'ApplyThemeForScreenshot' | head -40",
                ).stdout.trim().ifEmpty { "not listed" }
            }
            section("Default theme icons entries") {
                val bytes = shell.readFile("/system/media/theme/default/icons")
                if (bytes == null) {
                    "not readable"
                } else {
                    val entries = runCatching { MiuiIconsZip.read(bytes).keys.toList() }.getOrNull()
                    if (entries == null) "${bytes.size} bytes, not a zip" else "${bytes.size} bytes: " + entries.take(40).joinToString()
                }
            }
        }
    }

    private inline fun StringBuilder.section(title: String, body: () -> String) {
        appendLine("== $title")
        appendLine(runCatching(body).getOrElse { "failed: $it" })
    }

    /** Scans ThemeManager's DEX string tables (its APK is world-readable) for apply-related strings. */
    private fun themeManagerStrings(context: Context): String {
        val ai = context.packageManager.getApplicationInfo(ThemeManagerProbe.PACKAGE, 0)
        val apks = listOfNotNull(ai.sourceDir) + ai.splitSourceDirs.orEmpty()
        val found = LinkedHashSet<String>()
        var dexCount = 0
        for (apk in apks) {
            ZipFile(apk).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                    .forEach { entry ->
                        dexCount++
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        runCatching { DexStrings.read(bytes) }.getOrNull()?.let { found += DexStrings.interesting(it, limit = 400) }
                    }
            }
        }
        val list = DexStrings.interesting(found, limit = 150)
        return "APKs: ${apks.size}, dex files: $dexCount, matches: ${found.size}\n" + list.joinToString("\n")
    }
}

package com.iconshift.core.miui

import com.iconshift.core.applyengine.Requirement
import com.iconshift.core.shell.Shell
import com.iconshift.core.shell.ShellAccess

/**
 * How the HyperOS theme engine touches the system: read the applied icons, write the generated
 * theme package somewhere ThemeManager can read it, and ask ThemeManager to apply it.
 *
 * Two implementations exist: [ShellThemeHost] (Shizuku shell) and, on device, a direct host that
 * uses only normal app APIs, so the same theme logic can be tried with and without Shizuku.
 */
interface ThemeHost {
    /** Empty when the host can be used right now. */
    suspend fun missingRequirements(): List<Requirement>

    /** False when the host is momentarily unusable (e.g. the Shizuku service did not bind). */
    suspend fun available(): Boolean

    /** File contents, or null when missing/unreadable. */
    suspend fun readFile(path: String): ByteArray?

    /** Something that changes whenever the file changes (e.g. "mtime:size"), or null if absent. */
    suspend fun fileStamp(path: String): String?

    /** Writes the theme package and returns the path ThemeManager should open. Throws on failure. */
    suspend fun writeThemePackage(fileName: String, bytes: ByteArray): String

    /** Asks ThemeManager to apply [mtzPath] via [component]. */
    suspend fun launchApply(component: String, mtzPath: String): HostResult
}

data class HostResult(val ok: Boolean, val detail: String)

/** [ThemeHost] backed by the Shizuku shell (uid 2000). */
class ShellThemeHost(
    private val access: ShellAccess,
    private val outputDir: String = HyperOsThemeApplyEngine.DEFAULT_OUTPUT_DIR,
) : ThemeHost {

    override suspend fun missingRequirements() = access.missingRequirements()

    override suspend fun available() = access.shell() != null

    override suspend fun readFile(path: String): ByteArray? = access.shell()?.readFile(path)

    override suspend fun fileStamp(path: String): String? {
        val shell = access.shell() ?: return null
        val r = shell.exec("stat -c '%Y:%s' ${Shell.quote(path)}")
        return if (r.ok) r.stdout.trim().ifEmpty { null } else null
    }

    override suspend fun writeThemePackage(fileName: String, bytes: ByteArray): String {
        val shell = access.shell() ?: error("Privileged shell unavailable")
        val path = "$outputDir/$fileName"
        shell.writeFile(path, bytes)
        return path
    }

    override suspend fun launchApply(component: String, mtzPath: String): HostResult {
        val shell = access.shell() ?: return HostResult(false, "Privileged shell unavailable")
        val result = shell.exec(
            "am start -W -n ${Shell.quote(component)} " +
                "-e theme_file_path ${Shell.quote(mtzPath)} -e api_called_from test",
        )
        val refused = !result.ok || result.stdout.contains("Error") || result.stderr.contains("Error")
        return HostResult(!refused, "am: ${result.summary()}")
    }
}

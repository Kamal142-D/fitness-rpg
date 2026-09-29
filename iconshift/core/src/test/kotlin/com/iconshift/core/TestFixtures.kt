package com.iconshift.core

import com.iconshift.core.applyengine.Requirement
import com.iconshift.core.miui.MiuiIconsZip
import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.ShellAccess
import com.iconshift.core.shell.ShellResult

/** Minimal valid-looking PNG: signature + a marker so each fake icon hashes differently. */
fun fakePng(marker: String): ByteArray =
    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + marker.encodeToByteArray()

/**
 * In-memory shell. `am start` simulates ThemeManager applying an .mtz: it copies the package's
 * `icons` entry to [iconsPath]. `stat` reports a version counter that bumps on every write.
 */
class FakeShell(
    val iconsPath: String = "/data/system/theme/icons",
    var themeManagerWorks: Boolean = true,
    /** `am start` reports success but ThemeManager never applies anything. */
    var themeManagerIgnores: Boolean = false,
) : PrivilegedShell {
    val files = HashMap<String, ByteArray>()
    private val versions = HashMap<String, Int>()
    val commands = mutableListOf<String>()

    fun put(path: String, bytes: ByteArray) {
        files[path] = bytes
        versions[path] = (versions[path] ?: 0) + 1
    }

    override suspend fun exec(command: String): ShellResult {
        commands += command
        return when {
            command.startsWith("stat ") -> {
                val path = command.substringAfter("'%Y:%s' ").trim('\'')
                val v = versions[path] ?: return ShellResult(1, "", "No such file")
                ShellResult(0, "$v:${files[path]!!.size}\n", "")
            }
            command.startsWith("am start") -> {
                if (!themeManagerWorks) return ShellResult(0, "", "Error: Activity class does not exist.")
                if (themeManagerIgnores) return ShellResult(0, "Status: ok\n", "")
                val mtzPath = Regex("theme_file_path '([^']+)'").find(command)!!.groupValues[1]
                val mtz = MiuiIconsZip.read(files.getValue(mtzPath))
                put(iconsPath, mtz.getValue("icons"))
                ShellResult(0, "Status: ok\n", "")
            }
            else -> ShellResult(127, "", "unknown command")
        }
    }

    override suspend fun readFile(path: String): ByteArray? = files[path]

    override suspend fun writeFile(path: String, bytes: ByteArray) = put(path, bytes)
}

class FakeAccess(
    var shell: PrivilegedShell?,
    var missing: List<Requirement> = emptyList(),
) : ShellAccess {
    override suspend fun missingRequirements() = missing
    override suspend fun shell() = if (missing.isEmpty()) shell else null
}

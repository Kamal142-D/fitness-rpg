package com.iconshift.core.shell

/**
 * Shell-level (uid 2000) access, provided on device by a Shizuku user service.
 * Engines depend on this interface so their logic is testable off-device.
 */
interface PrivilegedShell {
    suspend fun exec(command: String): ShellResult

    /** File contents, or null when the file is missing or unreadable. */
    suspend fun readFile(path: String): ByteArray?

    /** Writes [bytes] to [path], creating parent directories. Throws on failure. */
    suspend fun writeFile(path: String, bytes: ByteArray)
}

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val ok: Boolean get() = exitCode == 0
    fun summary(): String = buildString {
        append("exit=").append(exitCode)
        if (stdout.isNotBlank()) append(" out=").append(stdout.trim().take(500))
        if (stderr.isNotBlank()) append(" err=").append(stderr.trim().take(500))
    }
}

object Shell {
    private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
    private val COMPONENT = Regex("^[A-Za-z][A-Za-z0-9_.]*/[A-Za-z0-9_.$]+$")

    /** Single-quotes [s] for `sh -c`. */
    fun quote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    fun isValidPackage(s: String): Boolean = PACKAGE.matches(s)

    fun isValidComponent(s: String): Boolean = COMPONENT.matches(s)
}

package com.iconshift.poc.shizuku

import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.Shell
import com.iconshift.core.shell.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.IOException
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * [PrivilegedShell] that runs commands directly in Shizuku's server process (as shell, uid 2000)
 * via `Shizuku.newProcess`. Unlike the user service, this needs no extra process of ours to be
 * started, which HyperOS 3 was observed to block/stall. `newProcess` is private in API 13, so it
 * is reached by reflection.
 */
class ShizukuProcessShell : PrivilegedShell {

    override suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        val p = start(command)
        runCatching { p.outputStream.close() }
        var stderr = ""
        val errReader = thread { stderr = runCatching { p.errorStream.bufferedReader().readText() }.getOrDefault("") }
        val stdout = runCatching { p.inputStream.bufferedReader().readText() }.getOrDefault("")
        val exit = waitFor(p)
        errReader.join(1_000)
        ShellResult(exit, stdout, stderr)
    }

    override suspend fun readFile(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val p = start("cat ${Shell.quote(path)}")
        runCatching { p.outputStream.close() }
        thread { runCatching { p.errorStream.readBytes() } }
        val bytes = runCatching { p.inputStream.readBytes() }.getOrNull()
        if (waitFor(p) == 0) bytes else null
    }

    override suspend fun writeFile(path: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val dir = path.substringBeforeLast('/', "")
        val cmd = (if (dir.isNotEmpty()) "mkdir -p ${Shell.quote(dir)} && " else "") + "cat > ${Shell.quote(path)}"
        val p = start(cmd)
        var stderr = ""
        val errReader = thread { stderr = runCatching { p.errorStream.bufferedReader().readText() }.getOrDefault("") }
        thread { runCatching { p.inputStream.readBytes() } }
        p.outputStream.use { it.write(bytes) }
        val exit = waitFor(p)
        errReader.join(1_000)
        if (exit != 0) throw IOException("write $path failed (exit $exit): ${stderr.trim()}")
    }

    private fun waitFor(p: Process): Int {
        val done = runCatching {
            if (p is ShizukuRemoteProcess) p.waitForTimeout(TIMEOUT_S, TimeUnit.SECONDS) else p.waitFor(TIMEOUT_S, TimeUnit.SECONDS)
        }.getOrDefault(false)
        if (!done) {
            runCatching { p.destroy() }
            return -2
        }
        return runCatching { p.exitValue() }.getOrDefault(-1)
    }

    private fun start(command: String): Process =
        NEW_PROCESS.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

    companion object {
        private const val TIMEOUT_S = 60L

        private val NEW_PROCESS: Method by lazy {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
        }
    }
}

package com.iconshift.poc.shizuku

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Shizuku user service. Runs in its own process as the shell user, so it can do what
 * `adb shell` can: run shell commands, read theme files, launch exported system activities,
 * and call OverlayManager with shell's CHANGE_OVERLAY_PACKAGES permission.
 *
 * Every method returns errors as values; nothing here reports success it did not observe.
 */
class PrivilegedService : IPrivilegedService.Stub() {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun uid(): Int = Process.myUid()

    override fun exec(command: String?): Bundle {
        val out = Bundle()
        if (command.isNullOrBlank()) {
            out.putInt(KEY_EXIT, -1)
            out.putString(KEY_ERR, "empty command")
            return out
        }
        try {
            val p = ProcessBuilder("sh", "-c", command).start()
            p.outputStream.close()
            var stderr = ""
            val errReader = thread { stderr = p.errorStream.bufferedReader().readText() }
            val stdout = p.inputStream.bufferedReader().readText()
            val finished = p.waitFor(EXEC_TIMEOUT_S, TimeUnit.SECONDS)
            errReader.join(1_000)
            if (!finished) {
                p.destroyForcibly()
                out.putInt(KEY_EXIT, -2)
                out.putString(KEY_OUT, stdout)
                out.putString(KEY_ERR, "timed out after ${EXEC_TIMEOUT_S}s\n$stderr")
                return out
            }
            out.putInt(KEY_EXIT, p.exitValue())
            out.putString(KEY_OUT, stdout)
            out.putString(KEY_ERR, stderr)
        } catch (e: Exception) {
            out.putInt(KEY_EXIT, -1)
            out.putString(KEY_ERR, e.toString())
        }
        return out
    }

    override fun openRead(path: String?): ParcelFileDescriptor? {
        if (path.isNullOrBlank()) return null
        return try {
            val f = File(path)
            if (!f.isFile) null else ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeFrom(path: String?, source: ParcelFileDescriptor?): String? {
        if (path.isNullOrBlank() || source == null) return "missing path or source"
        return try {
            val target = File(path)
            target.parentFile?.mkdirs()
            ParcelFileDescriptor.AutoCloseInputStream(source).use { input ->
                FileOutputStream(target).use { input.copyTo(it) }
            }
            target.setReadable(true, false)
            null
        } catch (e: Exception) {
            runCatching { source.close() }
            "write $path failed: $e"
        }
    }

    override fun registerIconOverlay(
        overlayName: String?,
        targetPackage: String?,
        resourceNames: Array<out String>?,
        png: ParcelFileDescriptor?,
        userId: Int,
    ): String? {
        if (overlayName.isNullOrBlank() || targetPackage.isNullOrBlank() || resourceNames.isNullOrEmpty() || png == null) {
            return "missing arguments"
        }
        if (Build.VERSION.SDK_INT < 34) return "fabricated overlays with file resources need Android 14+"
        val bytes = try {
            ParcelFileDescriptor.AutoCloseInputStream(png).use { it.readBytes() }
        } catch (e: Exception) {
            return "could not read icon: $e"
        }
        // Each resource entry needs its own descriptor; idmap2 consumes them independently.
        // Strategy 1: a world-readable temp file. Strategy 2: pipes. Report which failed.
        val errors = mutableListOf<String>()
        val tmp = File(TMP_DIR, "$overlayName.png")
        try {
            tmp.parentFile?.mkdirs()
            tmp.writeBytes(bytes)
            tmp.setReadable(true, false)
            val fds = resourceNames.map { it to ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY) }
            try {
                commitRegister(overlayName, targetPackage, fds, userId)
                return null
            } finally {
                fds.forEach { runCatching { it.second.close() } }
            }
        } catch (e: Throwable) {
            errors += "file-fd: ${describe(e)}"
        }
        try {
            val pipes = resourceNames.map { it to pipeOf(bytes) }
            try {
                commitRegister(overlayName, targetPackage, pipes, userId)
                return null
            } finally {
                pipes.forEach { runCatching { it.second.close() } }
            }
        } catch (e: Throwable) {
            errors += "pipe-fd: ${describe(e)}"
        }
        return errors.joinToString("\n")
    }

    override fun unregisterIconOverlay(overlayName: String?, userId: Int): String? {
        if (overlayName.isNullOrBlank()) return "missing overlay name"
        return try {
            val idCls = Class.forName("android.content.om.OverlayIdentifier")
            val identifier = idCls.getDeclaredConstructor(String::class.java, String::class.java)
                .apply { isAccessible = true }
                .newInstance(SHELL_PACKAGE, overlayName)
            commit { builder, builderCls ->
                builderCls.getMethod("unregisterFabricatedOverlay", idCls).invoke(builder, identifier)
            }
            File(TMP_DIR, "$overlayName.png").delete()
            null
        } catch (e: Throwable) {
            describe(e)
        }
    }

    // --- OverlayManager via reflection (hidden APIs, shell-owned overlays) ---

    private fun commitRegister(
        overlayName: String,
        targetPackage: String,
        resources: List<Pair<String, ParcelFileDescriptor>>,
        userId: Int,
    ) {
        val builderCls = Class.forName("android.content.om.FabricatedOverlay\$Builder")
        val builder = builderCls.getConstructor(String::class.java, String::class.java, String::class.java)
            .newInstance(SHELL_PACKAGE, overlayName, targetPackage)
        val setFile = builderCls.getMethod(
            "setResourceValue",
            String::class.java,
            ParcelFileDescriptor::class.java,
            String::class.java,
        )
        for ((name, fd) in resources) setFile.invoke(builder, name, fd, null)
        val overlay = builderCls.getMethod("build").invoke(builder)!!
        val identifier = overlay.javaClass.getMethod("getIdentifier").invoke(overlay)!!
        val overlayCls = Class.forName("android.content.om.FabricatedOverlay")
        val idCls = Class.forName("android.content.om.OverlayIdentifier")

        commit { tx, txCls ->
            txCls.getMethod("registerFabricatedOverlay", overlayCls).invoke(tx, overlay)
            val setEnabled = runCatching {
                txCls.getMethod("setEnabled", idCls, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            }.getOrNull()
            if (setEnabled != null) {
                setEnabled.invoke(tx, identifier, true, userId)
            } else {
                txCls.getMethod("setEnabled", idCls, Boolean::class.javaPrimitiveType).invoke(tx, identifier, true)
            }
        }
    }

    private fun commit(configure: (builder: Any, builderCls: Class<*>) -> Unit) {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, "overlay") as IBinder
        val om = Class.forName("android.content.om.IOverlayManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)!!
        val txBuilderCls = Class.forName("android.content.om.OverlayManagerTransaction\$Builder")
        val txBuilder = txBuilderCls.getConstructor().newInstance()
        configure(txBuilder, txBuilderCls)
        val tx = txBuilderCls.getMethod("build").invoke(txBuilder)
        val txCls = Class.forName("android.content.om.OverlayManagerTransaction")
        om.javaClass.getMethod("commit", txCls).invoke(om, tx)
    }

    private fun pipeOf(bytes: ByteArray): ParcelFileDescriptor {
        val (read, write) = ParcelFileDescriptor.createPipe()
        thread(isDaemon = true) {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(bytes) }
            } catch (_: Exception) {
                // Reader closed early; the commit error is reported by the caller.
            }
        }
        return read
    }

    private fun describe(e: Throwable): String {
        val root = if (e is InvocationTargetException) e.targetException ?: e else e
        return "${root.javaClass.name}: ${root.message}"
    }

    companion object {
        const val KEY_EXIT = "exit"
        const val KEY_OUT = "out"
        const val KEY_ERR = "err"
        private const val SHELL_PACKAGE = "com.android.shell"
        private const val TMP_DIR = "/data/local/tmp/iconshift"
        private const val EXEC_TIMEOUT_S = 60L
    }
}

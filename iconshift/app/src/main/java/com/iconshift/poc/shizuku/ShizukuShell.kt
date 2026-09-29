package com.iconshift.poc.shizuku

import android.os.ParcelFileDescriptor
import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.concurrent.thread

/** [PrivilegedShell] backed by the Shizuku user service. Large files travel over pipes/FDs, not binder buffers. */
class ShizukuShell(private val service: IPrivilegedService) : PrivilegedShell {

    override suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        val b = service.exec(command)
        ShellResult(
            exitCode = b.getInt(PrivilegedService.KEY_EXIT, -1),
            stdout = b.getString(PrivilegedService.KEY_OUT).orEmpty(),
            stderr = b.getString(PrivilegedService.KEY_ERR).orEmpty(),
        )
    }

    override suspend fun readFile(path: String): ByteArray? = withContext(Dispatchers.IO) {
        service.openRead(path)?.let { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        }
    }

    override suspend fun writeFile(path: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val error = withPipe(bytes) { service.writeFrom(path, it) }
        if (error != null) throw IOException(error)
    }
}

/**
 * Streams [bytes] through a pipe whose read end is handed to [block] (which passes it to the
 * service). The writer thread tolerates the reader closing early.
 */
fun <T> withPipe(bytes: ByteArray, block: (ParcelFileDescriptor) -> T): T {
    val (read, write) = ParcelFileDescriptor.createPipe()
    val writer = thread(isDaemon = true) {
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(bytes) }
        } catch (_: IOException) {
            // Reader went away; the service reports the failure.
        }
    }
    return try {
        block(read)
    } finally {
        runCatching { read.close() }
        writer.join(5_000)
    }
}

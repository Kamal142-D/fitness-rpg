package com.iconshift.poc.shizuku

import android.os.ParcelFileDescriptor
import java.io.IOException
import kotlin.concurrent.thread

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

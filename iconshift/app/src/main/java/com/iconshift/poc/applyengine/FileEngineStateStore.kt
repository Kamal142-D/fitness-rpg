package com.iconshift.poc.applyengine

import com.iconshift.core.applyengine.EngineStateStore
import java.io.File

/** Engine backups/bookkeeping as small files in app-private storage (survives restarts and reboots). */
class FileEngineStateStore(private val dir: File) : EngineStateStore {

    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_"))

    override fun get(key: String): ByteArray? = file(key).takeIf { it.isFile }?.readBytes()

    override fun put(key: String, value: ByteArray) {
        dir.mkdirs()
        val target = file(key)
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeBytes(value)
        if (!tmp.renameTo(target)) {
            target.writeBytes(value)
            tmp.delete()
        }
    }

    override fun remove(key: String) {
        file(key).delete()
    }
}

package com.iconshift.poc.applyengine

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.iconshift.core.applyengine.Requirement
import com.iconshift.core.miui.HostResult
import com.iconshift.core.miui.ThemeHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [ThemeHost] using only normal app APIs: no Shizuku, no root.
 * - Theme icons are world-readable, so they are read directly.
 * - The .mtz goes to Download/IconShift via MediaStore (no storage permission needed).
 * - ThemeManager's apply screen is opened with a regular startActivity, which only works when
 *   that activity is exported without a permission (checked by the engine's compatibility).
 */
class DirectThemeHost(private val context: Context) : ThemeHost {

    override suspend fun missingRequirements(): List<Requirement> = emptyList()

    override suspend fun available() = true

    override suspend fun readFile(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(path).takeIf { it.isFile && it.canRead() }?.readBytes() }.getOrNull()
    }

    override suspend fun fileStamp(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(path)
            if (f.exists()) "${f.lastModified()}:${f.length()}" else null
        }.getOrNull()
    }

    override suspend fun writeThemePackage(fileName: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        // Remove our earlier packages so Downloads doesn't fill up (only rows this app owns are visible here).
        runCatching {
            resolver.delete(
                collection,
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                arrayOf(RELATIVE_DIR, "iconshift_%.mtz"),
            )
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("MediaStore refused to create $fileName")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not open $uri")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        lastUri = uri
        // MediaStore may adjust the name; report the real path when it tells us.
        @Suppress("DEPRECATION")
        val actual = runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        actual ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IconShift/$fileName").path
    }

    @Volatile
    private var lastUri: Uri? = null

    override suspend fun launchApply(component: String, mtzPath: String): HostResult = withContext(Dispatchers.Main) {
        val cn = ComponentName.unflattenFromString(component)
            ?: return@withContext HostResult(false, "Bad component $component")
        val intent = Intent()
            .setComponent(cn)
            .putExtra("theme_file_path", mtzPath)
            .putExtra("api_called_from", "test")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Also offer the file as a readable content URI in case ThemeManager can't open the path.
        lastUri?.let { intent.setDataAndType(it, "application/octet-stream").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        try {
            context.startActivity(intent)
            HostResult(true, "startActivity($component) accepted")
        } catch (e: SecurityException) {
            HostResult(false, "ThemeManager blocks other apps from its apply screen: ${e.message}")
        } catch (e: ActivityNotFoundException) {
            HostResult(false, "ThemeManager apply screen not found: ${e.message}")
        }
    }

    private companion object {
        val RELATIVE_DIR = Environment.DIRECTORY_DOWNLOADS + "/IconShift/"
    }
}

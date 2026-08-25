package com.fitnessrpg.app.data.updates

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import java.io.File

/**
 * Downloads a release APK and hands it to the Android package installer. The
 * install itself is confirmed by the OS UI (and requires the user to allow
 * "install unknown apps" for this app the first time). Android only.
 */
object ApkInstaller {

    private const val APK_MIME = "application/vnd.android.package-archive"

    suspend fun downloadAndInstall(context: Context, apkUrl: String) {
        val client = HttpClient(Android)
        val file = File(context.cacheDir, "fitness-rpg-update.apk")
        try {
            val bytes: ByteArray = client.get(apkUrl).body()
            file.writeBytes(bytes)
        } finally {
            client.close()
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

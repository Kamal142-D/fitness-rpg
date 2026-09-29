package com.iconshift.poc.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Facts shown on the compatibility screen and in exported reports. */
data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val sdkInt: Int,
    val release: String,
    val securityPatch: String,
    val hyperOsName: String,
    val hyperOsCode: String,
    val hyperOsIncremental: String,
    val miuiVersionName: String,
    val miuiVersionCode: String,
    val launcherPackage: String,
    val launcherVersion: String,
) {
    val isHyperOs: Boolean get() = hyperOsName.isNotBlank()

    val isMiuiFamily: Boolean
        get() = isHyperOs || miuiVersionName.isNotBlank() ||
            manufacturer.equals("Xiaomi", ignoreCase = true)

    val osDescription: String
        get() = when {
            isHyperOs -> "HyperOS $hyperOsName ($hyperOsIncremental), Android $release"
            miuiVersionName.isNotBlank() -> "MIUI $miuiVersionName, Android $release"
            else -> "Android $release"
        }

    fun lines(): List<String> = listOf(
        "Device: $manufacturer $model ($device, brand $brand)",
        "Android: $release (SDK $sdkInt, patch $securityPatch)",
        "HyperOS: ${hyperOsName.ifBlank { "-" }} code=${hyperOsCode.ifBlank { "-" }} build=${hyperOsIncremental.ifBlank { "-" }}",
        "MIUI props: name=${miuiVersionName.ifBlank { "-" }} code=${miuiVersionCode.ifBlank { "-" }}",
        "Launcher: $launcherPackage $launcherVersion",
    )

    companion object {
        fun read(context: Context): DeviceInfo {
            val pm = context.packageManager
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val launcherPkg = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName.orEmpty()
            return DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND,
                model = Build.MODEL,
                device = Build.DEVICE,
                sdkInt = Build.VERSION.SDK_INT,
                release = Build.VERSION.RELEASE,
                securityPatch = Build.VERSION.SECURITY_PATCH,
                hyperOsName = getprop("ro.mi.os.version.name"),
                hyperOsCode = getprop("ro.mi.os.version.code"),
                hyperOsIncremental = getprop("ro.mi.os.version.incremental"),
                miuiVersionName = getprop("ro.miui.ui.version.name"),
                miuiVersionCode = getprop("ro.miui.ui.version.code"),
                launcherPackage = launcherPkg.ifBlank { "unknown" },
                launcherVersion = versionOf(pm, launcherPkg),
            )
        }

        fun versionOf(pm: PackageManager, pkg: String): String = runCatching {
            val info = pm.getPackageInfo(pkg, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("not installed")

        /** Reads a system property with the unprivileged `getprop` binary. Empty when unset/unreadable. */
        fun getprop(name: String): String = runCatching {
            val p = ProcessBuilder("getprop", name).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
        }.getOrDefault("")
    }
}

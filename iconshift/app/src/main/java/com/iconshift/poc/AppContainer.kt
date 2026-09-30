package com.iconshift.poc

import android.content.Context
import com.iconshift.core.applyengine.ApplyEngineRegistry
import com.iconshift.core.miui.HyperOsThemeApplyEngine
import com.iconshift.core.miui.HyperOsThemeEnvironment
import com.iconshift.core.miui.ShellThemeHost
import com.iconshift.poc.applyengine.DirectThemeHost
import com.iconshift.poc.applyengine.FabricatedOverlayApplyEngine
import com.iconshift.poc.applyengine.FileEngineStateStore
import com.iconshift.poc.device.DeviceInfo
import com.iconshift.poc.device.ThemeManagerProbe
import com.iconshift.poc.iconpack.IconPackRepository
import com.iconshift.poc.shizuku.ShizukuGate
import java.io.File

/** Manual wiring for the POC (Hilt arrives with the full app). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val shizuku = ShizukuGate(appContext)
    val iconPacks = IconPackRepository(appContext)
    private val store = FileEngineStateStore(File(appContext.filesDir, "engine-state"))

    @Volatile
    var device: DeviceInfo = DeviceInfo.read(appContext)
        private set

    @Volatile
    var themeManager: ThemeManagerProbe = ThemeManagerProbe.read(appContext)
        private set

    fun refreshProbes() {
        device = DeviceInfo.read(appContext)
        themeManager = ThemeManagerProbe.read(appContext)
    }

    private val themeEnvironment: suspend () -> HyperOsThemeEnvironment = {
        HyperOsThemeEnvironment(
            isMiuiFamily = device.isMiuiFamily,
            osDescription = device.osDescription,
            uiVersionCode = device.miuiVersionCode,
            applyComponent = themeManager.applyComponent,
            applyComponentPermission = themeManager.applyPermission,
        )
    }

    /** Tried first: needs nothing but ThemeManager's apply screen being open to all apps. */
    private val hyperOsDirectEngine = HyperOsThemeApplyEngine(
        host = DirectThemeHost(appContext),
        environment = themeEnvironment,
        store = store,
        direct = true,
    )

    private val hyperOsEngine = HyperOsThemeApplyEngine(
        host = ShellThemeHost(shizuku),
        environment = themeEnvironment,
        store = store,
    )

    private val overlayEngine = FabricatedOverlayApplyEngine(appContext, shizuku, store) { device.isMiuiFamily }

    val engines = ApplyEngineRegistry(listOf(hyperOsDirectEngine, hyperOsEngine, overlayEngine))
}

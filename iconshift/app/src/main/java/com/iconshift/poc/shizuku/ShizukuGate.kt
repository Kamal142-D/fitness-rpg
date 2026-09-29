package com.iconshift.poc.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.iconshift.core.applyengine.Requirement
import com.iconshift.core.shell.PrivilegedShell
import com.iconshift.core.shell.ShellAccess
import com.iconshift.poc.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

/** Tracks Shizuku state and owns the connection to [PrivilegedService]. */
class ShizukuGate(private val context: Context) : ShellAccess {

    enum class Status { NotInstalled, NotRunning, PermissionNeeded, Ready }

    private val _status = MutableStateFlow(computeStatus())
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    private var service: IPrivilegedService? = null
    private val bindLock = Mutex()

    private val args = Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, PrivilegedService::class.java.name))
        .daemon(false)
        .processNameSuffix("privileged")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    init {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
    }

    fun refresh() {
        _status.value = computeStatus()
    }

    fun requestPermission() {
        if (pingShizuku()) Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    /** 0 = root (Sui/root Shizuku), 2000 = shell (ADB/wireless-debugging Shizuku). */
    fun shizukuUid(): Int? = if (pingShizuku()) runCatching { Shizuku.getUid() }.getOrNull() else null

    fun shizukuVersion(): Int? = if (pingShizuku()) runCatching { Shizuku.getVersion() }.getOrNull() else null

    override suspend fun missingRequirements(): List<Requirement> = when (computeStatus()) {
        Status.NotInstalled -> listOf(Requirement.ShizukuInstalled, Requirement.ShizukuRunning, Requirement.ShizukuPermission)
        Status.NotRunning -> listOf(Requirement.ShizukuRunning, Requirement.ShizukuPermission)
        Status.PermissionNeeded -> listOf(Requirement.ShizukuPermission)
        Status.Ready -> emptyList()
    }

    override suspend fun shell(): PrivilegedShell? = service()?.let(::ShizukuShell)

    /** Connected privileged service, binding it on first use. Null if Shizuku isn't ready. */
    suspend fun service(): IPrivilegedService? {
        if (computeStatus() != Status.Ready) return null
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        return bindLock.withLock {
            service?.takeIf { it.asBinder().pingBinder() } ?: bind()
        }
    }

    private suspend fun bind(): IPrivilegedService? = withTimeoutOrNull(BIND_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val s = binder?.takeIf { it.pingBinder() }?.let { IPrivilegedService.Stub.asInterface(it) }
                    service = s
                    if (cont.isActive) cont.resume(s)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                }
            }
            try {
                Shizuku.bindUserService(args, connection)
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun computeStatus(): Status {
        if (!pingShizuku()) return if (isShizukuInstalled()) Status.NotRunning else Status.NotInstalled
        if (runCatching { Shizuku.isPreV11() }.getOrDefault(true)) return Status.NotRunning
        val granted = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        return if (granted) Status.Ready else Status.PermissionNeeded
    }

    private fun pingShizuku(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    private fun isShizukuInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    }.getOrDefault(false)

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val PERMISSION_REQUEST_CODE = 4201
        private const val BIND_TIMEOUT_MS = 10_000L
    }
}

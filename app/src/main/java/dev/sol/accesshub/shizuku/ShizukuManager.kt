package dev.sol.accesshub.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui

/**
 * Singleton wrapper around the Shizuku API.
 *
 * Registers the binder / permission listeners once (from the Application) and exposes the
 * resulting [ShizukuStatus] as a [StateFlow] the UI can collect. Every call is guarded so a
 * missing Shizuku installation degrades to [ShizukuState.NOT_INSTALLED] instead of crashing.
 */
object ShizukuManager {

    /**
     * The Shizuku manager app, plus the Sui variant that ships the same API bundle without
     * the manager app. Both are declared in the manifest `<queries>` so the lookup survives
     * Android 11 package visibility rules.
     */
    private val managerPackages = listOf(
        ShizukuProvider.MANAGER_APPLICATION_ID,
        "moe.shizuku.privilege.api",
    )
    private const val REQUEST_CODE = 0xB1B1

    private lateinit var appContext: Context

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    val isReady: Boolean get() = _status.value.isReady

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }

    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }

    private val permissionResult =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == REQUEST_CODE) refresh()
        }

    fun init(context: Context) {
        appContext = context.applicationContext
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceived)
            Shizuku.addBinderDeadListener(binderDead)
            Shizuku.addRequestPermissionResultListener(permissionResult)
        }
        refresh()
    }

    fun refresh() {
        if (!::appContext.isInitialized) return
        _status.value = compute()
    }

    /** Opens the Shizuku manager app; returns false when it is not installed. */
    fun launchManager(context: Context = appContext): Boolean {
        val intent = managerPackages.firstNotNullOfOrNull { pkg ->
            runCatching { context.packageManager.getLaunchIntentForPackage(pkg) }.getOrNull()
        } ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    /**
     * Triggers the Shizuku permission dialog when needed.
     * @return true when the permission is already granted or the dialog was raised.
     */
    fun requestPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                refresh()
                false
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refresh()
                true
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
                true
            }
        } catch (t: Throwable) {
            refresh()
            false
        }
    }

    private fun compute(): ShizukuStatus = try {
        // Resolved first: it is meaningful even while the binder is down.
        val provider = resolveProvider()
        val managerInstalled = isManagerInstalled()
        if (Shizuku.pingBinder()) {
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            ShizukuStatus(
                state = if (granted) ShizukuState.READY else ShizukuState.NO_PERMISSION,
                provider = provider,
                version = Shizuku.getVersion(),
                uid = Shizuku.getUid(),
            )
        } else {
            ShizukuStatus(
                state = if (managerInstalled || provider == PrivilegedProvider.SUI) {
                    ShizukuState.NOT_RUNNING
                } else {
                    ShizukuState.NOT_INSTALLED
                },
                provider = provider,
            )
        }
    } catch (t: Throwable) {
        ShizukuStatus(
            state = if (isManagerInstalled()) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
        )
    }

    /**
     * Sui ships no manager app, so an installed-but-idle Sui is only visible through
     * [Sui.isSui] (set by [Sui.init], which `ShizukuProvider` performs automatically).
     */
    private fun resolveProvider(): PrivilegedProvider = when {
        runCatching { Sui.isSui() }.getOrDefault(false) -> PrivilegedProvider.SUI
        isManagerInstalled() -> PrivilegedProvider.SHIZUKU
        else -> PrivilegedProvider.UNKNOWN
    }

    private fun isManagerInstalled(): Boolean = managerPackages.any { pkg ->
        runCatching { appContext.packageManager.getPackageInfo(pkg, 0) }.isSuccess
    }
}

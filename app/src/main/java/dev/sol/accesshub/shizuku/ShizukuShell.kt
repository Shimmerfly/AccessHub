package dev.sol.accesshub.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import dev.sol.accesshub.BuildConfig
import rikka.shizuku.Shizuku

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val success: Boolean get() = exitCode == 0 && stderr.isBlank()
    val errorMessage: String get() = stderr.ifBlank { "exit code $exitCode" }
}

/**
 * Runs shell commands in the Shizuku process (uid 2000/shell, or root when the binder was
 * started from a root manager such as Sui). The shell uid holds WRITE_SECURE_SETTINGS, which is
 * exactly what is needed to toggle accessibility services.
 *
 * Shizuku 13.x made `Shizuku.newProcess` private, so commands go through a bound
 * [ShellUserService] over an AIDL Binder instead of spawning a process directly.
 */
object ShizukuShell {

    private const val SEP = "\u0000"
    private const val BIND_TIMEOUT_MS = 10_000L

    private var appPackage: String = "dev.sol.accesshub"

    @Volatile
    private var bound: IShellService? = null
    private var connecting: CompletableDeferred<IShellService?>? = null
    private val mutex = Mutex()

    /** Called once from the Application with the real package name. */
    fun init(packageName: String) {
        appPackage = packageName
    }

    /** Single-quote a value so it survives `sh -c` intact. */
    fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = IShellService.Stub.asInterface(binder)
            bound = svc
            connecting?.complete(svc)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = null
        }

        override fun onBindingDied(name: ComponentName?) {
            bound = null
            connecting?.complete(null)
        }

        override fun onNullBinding(name: ComponentName?) {
            bound = null
            connecting?.complete(null)
        }
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(appPackage, ShellUserService::class.java.name))
            .daemon(false)
            // An explicit tag keeps the identity stable when R8 renames the class.
            .tag("accesshub_shell")
            .processNameSuffix("shell")
            .debuggable(false)
            // Ties the shell-side service to this build, so an app update rebinds it.
            .version(BuildConfig.VERSION_CODE)

    /** Returns the bound service, binding on first use and re-binding if the binder died. */
    private suspend fun ensureBound(): IShellService? = mutex.withLock {
        bound?.let { return@withLock it }
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            return@withLock null
        }
        val deferred = CompletableDeferred<IShellService?>()
        connecting = deferred

        // Ensure clean state for this exact argument set.
        val args = userServiceArgs()
        runCatching { Shizuku.unbindUserService(args, connection, true) }

        val ok = runCatching {
            Shizuku.bindUserService(args, connection)
        }.isSuccess
        if (!ok) {
            connecting = null
            return@withLock null
        }
        val svc = withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
        if (svc == null) {
            runCatching { Shizuku.unbindUserService(args, connection, true) }
        }
        connecting = null
        svc
    }

    suspend fun exec(script: String): ShellResult = withContext(Dispatchers.IO) {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            return@withContext ShellResult(-1, "", "Shizuku/Sui service is not running")
        }
        val svc = ensureBound()
            ?: return@withContext ShellResult(-1, "", "Shizuku/Sui service unavailable (bind failed or timed out)")
        val raw = runCatching { svc.exec(script) }.getOrElse {
            // Binder likely died mid-call; drop it so the next exec re-binds.
            bound = null
            return@withContext ShellResult(-1, "", it.message ?: "Shizuku/Sui execution failed")
        }
        parse(raw)
    }

    private fun parse(raw: String): ShellResult {
        val parts = raw.split(SEP)
        val code = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: -1
        val out = parts.getOrNull(1)?.trim().orEmpty()
        val err = parts.getOrNull(2)?.trim().orEmpty()
        return ShellResult(code, out, err)
    }
}

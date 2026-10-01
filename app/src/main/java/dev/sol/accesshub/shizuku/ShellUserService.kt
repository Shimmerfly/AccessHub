package dev.sol.accesshub.shizuku

import androidx.annotation.Keep

/**
 * User service that lives inside the Shizuku process (shell uid 2000, which holds
 * WRITE_SECURE_SETTINGS). Shizuku loads the app APK into its own process and instantiates this
 * class.
 *
 * CRITICAL: This class MUST NOT extend `android.app.Service`! Shizuku instantiates it via
 * reflection in a raw Java process without an Android Context. It must directly implement the
 * AIDL Stub (which is an IBinder) and have a public no-argument constructor.
 */
@Keep
class ShellUserService : IShellService.Stub() {

    override fun exec(script: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", script))
            // Drain both pipes on their own threads so a full stderr buffer cannot deadlock.
            var stdout = ""
            var stderr = ""
            val outThread = Thread {
                stdout = runCatching { process.inputStream.bufferedReader().use { it.readText() } }
                    .getOrDefault("")
            }
            val errThread = Thread {
                stderr = runCatching { process.errorStream.bufferedReader().use { it.readText() } }
                    .getOrDefault("")
            }
            outThread.start()
            errThread.start()
            val code = process.waitFor()
            outThread.join()
            errThread.join()
            "$code$SEP$stdout$SEP$stderr"
        } catch (t: Throwable) {
            "-1$SEP$SEP${t.message ?: "exec failed"}"
        }
    }

    override fun destroy() {
        // Shizuku asks the old instance to clean up (it keeps running otherwise, see the
        // Shizuku-API README), so the shell process exits on its own.
        System.exit(0)
    }

    private companion object {
        const val SEP = "\u0000"
    }
}

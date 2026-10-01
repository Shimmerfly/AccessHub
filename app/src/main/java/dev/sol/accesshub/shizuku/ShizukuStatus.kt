package dev.sol.accesshub.shizuku

import androidx.compose.runtime.Immutable

/** Coarse-grained connection state of the privileged binder. */
enum class ShizukuState {
    UNKNOWN,
    NOT_INSTALLED,
    NOT_RUNNING,
    NO_PERMISSION,
    READY,
}

/**
 * Which implementation provides the Shizuku API.
 *
 * [displayName] is a product name, so it stays untranslated; [UNKNOWN] keeps it empty and the
 * UI falls back to a localized "Shizuku / Sui" label.
 */
enum class PrivilegedProvider(val displayName: String) {
    UNKNOWN(""),
    SHIZUKU("Shizuku"),
    SUI("Sui"),
}

/**
 * Snapshot of the privileged service behind the home status card.
 *
 * [version] / [uid] are only meaningful while the binder is alive, which is why they default
 * to 0 / -1: the card reads the uid to tell an ADB (shell, 2000) start from a root one.
 */
@Immutable
data class ShizukuStatus(
    val state: ShizukuState = ShizukuState.UNKNOWN,
    val provider: PrivilegedProvider = PrivilegedProvider.UNKNOWN,
    val version: Int = 0,
    val uid: Int = -1,
) {
    val isReady: Boolean
        get() = state == ShizukuState.READY

    /** Binder alive, permission may still be missing. */
    val isRunning: Boolean
        get() = state == ShizukuState.READY || state == ShizukuState.NO_PERMISSION

    val isInstalled: Boolean
        get() = isRunning || state == ShizukuState.NOT_RUNNING

    val isRoot: Boolean
        get() = isRunning && uid == 0
}

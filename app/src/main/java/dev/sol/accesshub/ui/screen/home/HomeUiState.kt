package dev.sol.accesshub.ui.screen.home

import androidx.compose.runtime.Immutable
import dev.sol.accesshub.shizuku.ShizukuState
import dev.sol.accesshub.shizuku.ShizukuStatus
import dev.sol.accesshub.ui.util.LatestVersionInfo

@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean = false,
    val latestVersionInfo: LatestVersionInfo = LatestVersionInfo(),
    val currentAppVersionCode: Long = 0L,
    val shizuku: ShizukuStatus = ShizukuStatus(),
    val accessibility: AccessibilityStatus = AccessibilityStatus(),
    val systemInfo: SystemInfo = SystemInfo(),
) {
    val hasUpdate: Boolean
        get() = latestVersionInfo.versionCode > currentAppVersionCode

    val showShizukuUnknown: Boolean
        get() = shizuku.state == ShizukuState.UNKNOWN

    val showShizukuNotInstalled: Boolean
        get() = shizuku.state == ShizukuState.NOT_INSTALLED

    val showShizukuNotRunning: Boolean
        get() = shizuku.state == ShizukuState.NOT_RUNNING

    val showShizukuNoPermission: Boolean
        get() = shizuku.state == ShizukuState.NO_PERMISSION
}

@Immutable
data class HomeActions(
    val onOpenUrl: (String) -> Unit = {},
    val onRequestShizukuPermission: () -> Unit = {},
    val onOpenShizuku: () -> Unit = {},
)

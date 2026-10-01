package dev.sol.accesshub.ui.screen.home

import androidx.compose.runtime.Immutable
import dev.sol.accesshub.ui.util.LatestVersionInfo

@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentAppVersionCode: Long,
    val systemInfo: SystemInfo,
)

@Immutable
data class HomeActions(
    val onPermissionsClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
)

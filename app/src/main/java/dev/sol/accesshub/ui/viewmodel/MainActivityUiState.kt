package dev.sol.accesshub.ui.viewmodel

import androidx.compose.runtime.Immutable
import dev.sol.accesshub.ui.UiMode
import dev.sol.accesshub.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val uiMode: UiMode,
)

package dev.sol.accesshub.ui.screen.accessibility

import androidx.compose.runtime.Immutable
import dev.sol.accesshub.data.ServiceMeta

@Immutable
data class AccessibilityUiState(
    val services: List<ServiceMeta> = emptyList(),
    val enabledIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    /** True while a privileged backend (Shizuku or Sui) answers, so the toggles can be written. */
    val writable: Boolean = false,
    val error: String? = null,
)

@Immutable
data class AccessibilityActions(
    val onToggle: (String, Boolean) -> Unit = { _, _ -> },
    val onRefresh: () -> Unit = {},
    val onRequestShizukuPermission: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

package dev.sol.accesshub.ui.screen.accessibility

import androidx.compose.runtime.Immutable
import dev.sol.accesshub.data.ServiceMeta
import dev.sol.accesshub.ui.component.SearchStatus

@Immutable
data class AccessibilityUiState(
    val services: List<ServiceMeta> = emptyList(),
    /** Filtered by [searchStatus]; what the search view shows. */
    val searchResults: List<ServiceMeta> = emptyList(),
    val enabledIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    /** True while a privileged backend (Shizuku or Sui) answers, so the toggles can be written. */
    val writable: Boolean = false,
    val error: String? = null,
    /** When true the system-app section is not rendered at all (settings-driven). */
    val hideSystemApps: Boolean = false,
    /** How many lines of an expanded service description to show (settings-driven). */
    val serviceDescriptionMaxLines: Int = 4,
    /** Services whose description is currently unfolded. Cleared whenever the page is re-entered. */
    val expandedDescriptionIds: Set<String> = emptySet(),
    val searchStatus: SearchStatus = SearchStatus(""),
)

@Immutable
data class AccessibilityActions(
    val onToggle: (String, Boolean) -> Unit = { _, _ -> },
    val onToggleDescription: (String) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onRequestShizukuPermission: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onSearchStatusChange: (SearchStatus) -> Unit = {},
    val onSearchTextChange: (String) -> Unit = {},
    val onClearSearch: () -> Unit = {},
)

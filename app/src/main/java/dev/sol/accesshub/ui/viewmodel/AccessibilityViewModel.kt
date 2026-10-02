package dev.sol.accesshub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.sol.accesshub.accessHubApp
import dev.sol.accesshub.data.ServiceMeta
import dev.sol.accesshub.data.repository.AccessibilityRepository
import dev.sol.accesshub.data.repository.SettingsRepository
import dev.sol.accesshub.data.repository.SettingsRepositoryImpl
import dev.sol.accesshub.shizuku.ShizukuManager
import dev.sol.accesshub.ui.component.SearchStatus
import dev.sol.accesshub.ui.screen.accessibility.AccessibilityUiState

class AccessibilityViewModel(
    private val repository: AccessibilityRepository = AccessibilityRepository(accessHubApp),
    private val settings: SettingsRepository = SettingsRepositoryImpl(),
) : ViewModel() {

    // Seed from the settings store synchronously: reading them in refresh() is async, so the first
    // frame used stale values and the system-app section flashed in and out after the switch moved.
    private val _uiState = MutableStateFlow(
        AccessibilityUiState(
            hideSystemApps = settings.hideSystemApps,
            serviceDescriptionMaxLines = settings.serviceDescriptionMaxLines,
        )
    )
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    /** Debounced through [launchSearchQueryCollector], like upstream's module list. */
    private val searchQuery = MutableStateFlow("")

    init {
        // Toggles are only usable while the privileged binder answers.
        viewModelScope.launch {
            ShizukuManager.status.collect { status ->
                _uiState.update { it.copy(writable = status.isReady) }
            }
        }
        viewModelScope.launchSearchQueryCollector(searchQuery) { applySearchText(it) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val loaded = withContext(Dispatchers.IO) {
                repository.installedServices() to repository.readEnabled()
            }
            val searchText = _uiState.value.searchStatus.searchText
            // The top of the slider means "show everything", i.e. no line cap at all.
            val hideSystemApps = settings.hideSystemApps
            val storedMaxLines = settings.serviceDescriptionMaxLines
            val descriptionMaxLines = if (storedMaxLines >= 10) Int.MAX_VALUE else storedMaxLines
            _uiState.update {
                it.copy(
                    services = loaded.first,
                    serviceDescriptionMaxLines = descriptionMaxLines,
                    hideSystemApps = hideSystemApps,
                    searchResults = filterServices(loaded.first, searchText),
                    enabledIds = loaded.second.services,
                    writable = ShizukuManager.isReady,
                    loading = false,
                    error = null,
                    searchStatus = it.searchStatus.copy(
                        resultStatus = searchResultStatusFor(
                            searchText,
                            filterServices(loaded.first, searchText).isEmpty()
                        )
                    ),
                )
            }
        }
    }

    /**
     * The system stores the whole enabled set in one setting, so a toggle rewrites the complete
     * list. The state is updated optimistically (the written value is authoritative, and the
     * framework's own flag follows it) and rolled back when the privileged call fails.
     */
    fun setEnabled(id: String, enabled: Boolean) {
        val before = _uiState.value.enabledIds
        val next = if (enabled) before + id else before - id
        _uiState.update { it.copy(enabledIds = next, error = null) }
        viewModelScope.launch {
            val result = repository.writeEnabled(next)
            if (!result.success) {
                _uiState.update { it.copy(enabledIds = before, error = result.errorMessage) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun updateSearchStatus(status: SearchStatus) {
        val previous = _uiState.value.searchStatus
        _uiState.update { it.copy(searchStatus = status) }
        if (previous.searchText != status.searchText) {
            searchQuery.value = status.searchText
        }
    }

    fun updateSearchText(text: String) {
        updateSearchStatus(_uiState.value.searchStatus.copy(searchText = text))
    }

    fun clearSearch() {
        updateSearchStatus(
            _uiState.value.searchStatus.copy(
                searchText = "",
                current = SearchStatus.Status.COLLAPSED,
            )
        )
    }

    private fun filterServices(services: List<ServiceMeta>, text: String): List<ServiceMeta> {
        if (text.isEmpty()) return emptyList()

        return services.filter {
            it.label.contains(text, true) ||
                it.appName.contains(text, true) ||
                it.pkg.contains(text, true) ||
                it.description?.contains(text, true) == true
        }
    }

    private suspend fun applySearchText(text: String) {
        _uiState.update {
            it.copy(
                searchStatus = it.searchStatus.copy(resultStatus = searchLoadingStatusFor(text))
            )
        }

        if (text.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        val result = withContext(Dispatchers.IO) {
            filterServices(_uiState.value.services, text)
        }

        _uiState.update {
            it.copy(
                searchResults = result,
                searchStatus = it.searchStatus.copy(
                    resultStatus = searchResultStatusFor(text, result.isEmpty())
                ),
            )
        }
    }
}

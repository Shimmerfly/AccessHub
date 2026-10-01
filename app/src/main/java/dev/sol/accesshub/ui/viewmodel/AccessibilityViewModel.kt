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
import dev.sol.accesshub.data.repository.AccessibilityRepository
import dev.sol.accesshub.shizuku.ShizukuManager
import dev.sol.accesshub.ui.screen.accessibility.AccessibilityUiState

class AccessibilityViewModel(
    private val repository: AccessibilityRepository = AccessibilityRepository(accessHubApp),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    init {
        // Toggles are only usable while the privileged binder answers.
        viewModelScope.launch {
            ShizukuManager.status.collect { status ->
                _uiState.update { it.copy(writable = status.isReady) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val loaded = withContext(Dispatchers.IO) {
                repository.installedServices() to repository.readEnabled()
            }
            _uiState.update {
                it.copy(
                    services = loaded.first,
                    enabledIds = loaded.second.services,
                    writable = ShizukuManager.isReady,
                    loading = false,
                    error = null,
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
}

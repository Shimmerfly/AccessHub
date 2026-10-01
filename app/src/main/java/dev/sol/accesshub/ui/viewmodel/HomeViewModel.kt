package dev.sol.accesshub.ui.viewmodel

import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import dev.sol.accesshub.data.repository.SettingsRepository
import dev.sol.accesshub.data.repository.SettingsRepositoryImpl
import dev.sol.accesshub.shizuku.ShizukuManager
import dev.sol.accesshub.ui.screen.home.AccessibilityStatus
import dev.sol.accesshub.ui.screen.home.HomeUiState
import dev.sol.accesshub.ui.screen.home.SystemInfo
import dev.sol.accesshub.ui.screen.home.getAppVersion
import dev.sol.accesshub.ui.screen.home.resolveAndroidVersion
import dev.sol.accesshub.ui.screen.home.resolveDeviceName
import dev.sol.accesshub.ui.util.LatestVersionInfo
import dev.sol.accesshub.ui.util.checkNewVersion

class HomeViewModel(
    private val settingsRepo: SettingsRepository = SettingsRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // The status card follows the binder, so mirror every Shizuku state change.
        viewModelScope.launch {
            ShizukuManager.status.collect { status ->
                _uiState.update { it.copy(shizuku = status) }
            }
        }
    }

    fun refresh() {
        ShizukuManager.refresh()
        viewModelScope.launch {
            val baseState = withContext(Dispatchers.IO) { buildState() }
            _uiState.update { baseState }
            if (baseState.checkUpdateEnabled) {
                val latestVersionInfo = withContext(Dispatchers.IO) { checkNewVersion() }
                _uiState.update { it.copy(latestVersionInfo = latestVersionInfo) }
            }
        }
    }

    private fun buildState(): HomeUiState {
        val appVersion = getAppVersion(accessHubApp)

        return HomeUiState(
            checkUpdateEnabled = settingsRepo.checkUpdate,
            latestVersionInfo = LatestVersionInfo(),
            currentAppVersionCode = appVersion.versionCode,
            shizuku = ShizukuManager.status.value,
            accessibility = readAccessibilityStatus(),
            systemInfo = SystemInfo(
                appVersion = "${appVersion.versionName} (${appVersion.versionCode})",
                androidVersion = resolveAndroidVersion(),
                deviceModel = resolveDeviceName(),
                fingerprint = Build.FINGERPRINT,
            ),
        )
    }

    /**
     * Reads the enabled set straight from `Settings.Secure`, which needs no permission, so the
     * home page stays truthful even before the user grants Shizuku access.
     */
    private fun readAccessibilityStatus(): AccessibilityStatus {
        val context = accessHubApp
        val installed = runCatching {
            context.getSystemService(AccessibilityManager::class.java)?.installedAccessibilityServiceList
        }.getOrNull().orEmpty()

        val resolver = context.contentResolver
        val raw = runCatching {
            Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        val enabled = raw.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" }
            .toSet()
        val accessibilityEnabled = runCatching {
            Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        }.getOrDefault(0) == 1

        return AccessibilityStatus(
            installedCount = installed.size,
            enabledCount = enabled.size,
            accessibilityEnabled = accessibilityEnabled,
            viaShizuku = ShizukuManager.isReady,
        )
    }
}

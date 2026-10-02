package dev.sol.accesshub.ui.screen.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.sol.accesshub.shizuku.ShizukuManager
import dev.sol.accesshub.ui.LocalUiMode
import dev.sol.accesshub.ui.UiMode
import dev.sol.accesshub.ui.viewmodel.AccessibilityViewModel

@Composable
fun AccessibilityPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<AccessibilityViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    // Refresh whenever this page becomes current, not just the first time: the theme page writes
    // `serviceDescriptionMaxLines` while this page stays composed inside the pager, so the old
    // one-shot activation guard kept the rows on the stale line count.
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) viewModel.refresh()
    }

    // Coming back from the system accessibility settings must re-read the enabled set.
    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }

    val actions = AccessibilityActions(
        onToggle = viewModel::setEnabled,
        onRefresh = viewModel::refresh,
        onRequestShizukuPermission = { ShizukuManager.requestPermission() },
        onDismissError = viewModel::dismissError,
        onSearchStatusChange = viewModel::updateSearchStatus,
        onSearchTextChange = viewModel::updateSearchText,
        onClearSearch = viewModel::clearSearch,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AccessibilityPagerMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> AccessibilityPagerMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.sol.accesshub.ui.component.dialog.rememberConfirmDialog
import dev.sol.accesshub.R

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
        if (isCurrentPage) viewModel.refresh() else viewModel.collapseDescriptions()
    }

    // Fold the descriptions the moment this page goes away, not on the way back in: pushing the
    // theme screen disposes this composition without the pager flag ever turning false.
    DisposableEffect(Unit) {
        onDispose { viewModel.collapseDescriptions() }
    }

    // Coming back from the system accessibility settings must re-read the enabled set.
    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }

    // TalkBack rewrites how the whole system reacts to input, so enabling it asks for confirmation.
    val pendingTalkBack = remember { mutableStateOf<String?>(null) }
    val talkBackTitle = stringResource(R.string.accessibility_talkback_warning_title)
    val talkBackMessage = stringResource(R.string.accessibility_talkback_warning)
    val talkBackConfirm = stringResource(R.string.accessibility_talkback_enable_anyway)
    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            pendingTalkBack.value?.let { viewModel.setEnabled(it, true) }
            pendingTalkBack.value = null
        },
        onDismiss = { pendingTalkBack.value = null },
    )

    val actions = AccessibilityActions(
        onToggle = { id, enabled ->
            if (enabled && id.contains("talkback", ignoreCase = true)) {
                pendingTalkBack.value = id
                confirmDialog.showConfirm(
                    title = talkBackTitle,
                    content = talkBackMessage,
                    markdown = false,
                    html = false,
                    confirm = talkBackConfirm,
                    dismiss = null,
                    isWarning = true,
                )
            } else {
                viewModel.setEnabled(id, enabled)
            }
        },
        onToggleDescription = viewModel::toggleDescription,
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

package dev.sol.accesshub.ui.screen.accessibility

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sol.accesshub.R
import dev.sol.accesshub.data.ServiceMeta
import dev.sol.accesshub.data.repository.SettingsRepositoryImpl
import dev.sol.accesshub.ui.component.ScrollToTopOnChange
import dev.sol.accesshub.ui.component.WarningLevel
import dev.sol.accesshub.ui.component.material.ExpressiveSwitch
import dev.sol.accesshub.ui.component.material.SearchAppBar
import dev.sol.accesshub.ui.component.material.SegmentedItem
import dev.sol.accesshub.ui.component.material.SegmentedListItem
import dev.sol.accesshub.ui.component.material.TonalCard

@Composable
fun AccessibilityPagerMaterial(
    state: AccessibilityUiState,
    actions: AccessibilityActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = remember { SnackbarHostState() }
    val searchListState = rememberLazyListState()
    // System-app services live in their own section, folded away on first look.
    // The ViewModel's copy can lag one async refresh behind, and reading it on the first frame made
    // the system-app section flash after the switch was toggled on the theme page. Read it straight
    // from the settings store instead: this page is recomposed from scratch whenever it is re-entered.
    val hideSystemApps = remember { SettingsRepositoryImpl().hideSystemApps }
    // The ViewModel's copy only lands after its async refresh, so read the clamp straight from the
    // settings store as well: a slider change must apply the moment the page is composed again.
    val descriptionMaxLines = remember { SettingsRepositoryImpl().serviceDescriptionMaxLines }
        .let { if (it >= 10) Int.MAX_VALUE else it }

    var systemAppsExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = { Text(stringResource(R.string.home_accessibility_services)) },
                searchText = state.searchStatus.searchText,
                onSearchTextChange = actions.onSearchTextChange,
                onClearClick = actions.onClearSearch,
                snackbarHostState = snackBarHost,
                scrollBehavior = scrollBehavior,
                searchContent = { bottomPadding, _ ->
                    val latestResults = rememberUpdatedState(state.searchResults)
                    ScrollToTopOnChange(searchListState, state.searchStatus.searchText) {
                        latestResults.value
                    }
                    LazyColumn(
                        state = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 13.dp,
                            bottom = 16.dp + bottomPadding,
                        ),
                    ) {
                        if (state.searchResults.isEmpty()) {
                            item(key = "search_empty") {
                                EmptyCard(title = stringResource(R.string.accessibility_search_empty))
                            }
                        }
                        itemsIndexed(state.searchResults, key = { _, item -> item.id }) { index, service ->
                            SegmentedItem(index = index, count = state.searchResults.size) {
                                ServiceRow(
                                    service = service,
                                    enabled = service.id in state.enabledIds,
                                    writable = state.writable,
                                    descriptionMaxLines = descriptionMaxLines,
                                    expanded = service.id in state.expandedDescriptionIds,
                                    onToggleDescription = { actions.onToggleDescription(service.id) },
                                    onToggle = { actions.onToggle(service.id, it) },
                                )
                            }
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 13.dp,
                bottom = 16.dp + bottomInnerPadding,
            ),
        ) {
            if (state.error != null) {
                item(key = "error") {
                    WarningCard(
                        message = stringResource(R.string.accessibility_toggle_failed, state.error),
                        onClick = actions.onDismissError,
                    )
                }
            }
            if (!state.writable) {
                item(key = "permission") {
                    WarningCard(
                        message = stringResource(R.string.accessibility_need_permission),
                        level = WarningLevel.Notice,
                        onClick = actions.onRequestShizukuPermission,
                    )
                }
            }
            if (state.services.isEmpty() && !state.loading) {
                item(key = "empty") {
                    EmptyCard(
                        title = stringResource(R.string.accessibility_empty),
                        summary = stringResource(R.string.accessibility_empty_summary),
                    )
                }
            }
            // Third-party services first; the system ones sit in a folded section underneath.
            val userServices = state.services.filter { !it.isSystemApp }
            val systemServices = state.services.filter { it.isSystemApp }
            itemsIndexed(userServices, key = { _, item -> item.id }) { index, service ->
                SegmentedItem(index = index, count = userServices.size) {
                    ServiceRow(
                        service = service,
                        enabled = service.id in state.enabledIds,
                        writable = state.writable,
                        descriptionMaxLines = descriptionMaxLines,
                        expanded = service.id in state.expandedDescriptionIds,
                        onToggleDescription = { actions.onToggleDescription(service.id) },
                        onToggle = { actions.onToggle(service.id, it) },
                    )
                }
            }
            if (!hideSystemApps && systemServices.isNotEmpty()) {
                // A gap marks the section break from the user apps above.
                item(key = "system_apps_gap") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // One item holds the whole section: that lets it animate open, and keeps the header
                // on the small inner radius where it meets the rows underneath.
                item(key = "system_apps_group") {
                    val groupCount = if (systemAppsExpanded) systemServices.size + 1 else 1
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SegmentedItem(index = 0, count = groupCount) {
                            SystemAppsHeader(
                                count = systemServices.size,
                                expanded = systemAppsExpanded,
                                onToggle = { systemAppsExpanded = !systemAppsExpanded },
                            )
                        }
                        AnimatedVisibility(visible = systemAppsExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                systemServices.forEachIndexed { index, service ->
                                    SegmentedItem(index = index + 1, count = groupCount) {
                                        ServiceRow(
                                            service = service,
                                            enabled = service.id in state.enabledIds,
                                            writable = state.writable,
                                            descriptionMaxLines = descriptionMaxLines,
                                            expanded = service.id in state.expandedDescriptionIds,
                                            onToggleDescription = { actions.onToggleDescription(service.id) },
                                            onToggle = { actions.onToggle(service.id, it) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Header of the collapsible system-app section; the chevron flips while it opens. */
@Composable
private fun SystemAppsHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "systemAppsChevron",
    )

    SegmentedListItem(
        onClick = onToggle,
        headlineContent = {
            Text(
                text = "${stringResource(R.string.accessibility_system_apps)} ($count)",
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun WarningCard(
    message: String,
    level: WarningLevel = WarningLevel.Error,
    onClick: (() -> Unit)? = null,
) {
    val containerColor = when (level) {
        WarningLevel.Error -> MaterialTheme.colorScheme.errorContainer
        WarningLevel.Notice -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.contentColorFor(containerColor)
            )
        }
    }
    if (onClick != null) {
        TonalCard(containerColor = containerColor, onClick = onClick, content = content)
    } else {
        TonalCard(containerColor = containerColor, content = content)
    }
}

@Composable
private fun EmptyCard(
    title: String,
    summary: String? = null,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One service, laid out like upstream's superuser row: the app icon, the service name with its
 * package underneath, and the enable switch on the right. Tapping the row expands the
 * accessibility description; the switch only toggles the service.
 */
@Composable
private fun ServiceRow(
    service: ServiceMeta,
    enabled: Boolean,
    writable: Boolean,
    descriptionMaxLines: Int,
    expanded: Boolean,
    onToggleDescription: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        SegmentedListItem(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onToggleDescription()
            },
            headlineContent = {
                Text(
                    text = service.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                // No style override: the description inherits the slot's own typography, which is the
                // size the package name used to have, and it shares the container colour.
                Text(
                    text = service.description
                        ?: stringResource(R.string.accessibility_no_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else descriptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                )
            },
            leadingContent = {
                service.icon?.let { icon ->
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }
            },
            trailingContent = {
                ExpressiveSwitch(
                    enabled = writable,
                    checked = enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onToggle(it)
                    },
                )
            },
        )
    }
}

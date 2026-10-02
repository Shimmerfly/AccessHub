package dev.sol.accesshub.ui.screen.accessibility

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sol.accesshub.R
import dev.sol.accesshub.data.ServiceMeta
import dev.sol.accesshub.data.repository.SettingsRepositoryImpl
import dev.sol.accesshub.ui.component.SearchStatus
import dev.sol.accesshub.ui.component.WarningLevel
import dev.sol.accesshub.ui.component.miuix.SearchBarFake
import dev.sol.accesshub.ui.component.miuix.SearchBox
import dev.sol.accesshub.ui.component.miuix.SearchPager
import dev.sol.accesshub.ui.component.miuix.WarningCard
import dev.sol.accesshub.ui.theme.LocalEnableBlur
import dev.sol.accesshub.ui.util.BlurredBar
import dev.sol.accesshub.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AccessibilityPagerMiuix(
    state: AccessibilityUiState,
    actions: AccessibilityActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    // The localized hint lives only in the UI; the stored status keeps upstream's empty label.
    val searchStatus = state.searchStatus.copy(label = stringResource(R.string.accessibility_search_hint))
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
            BlurredBar(backdrop) {
                Column {
                    searchStatus.TopAppBarAnim(backgroundColor = barColor) {
                        TopAppBar(
                            color = barColor,
                            title = stringResource(R.string.home_accessibility_services),
                            scrollBehavior = scrollBehavior
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    actions.onSearchStatusChange(
                                        state.searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                    )
                                }
                            }
                    ) {
                        SearchBarFake(searchStatus.label, 12.dp)
                    }
                }
            }
        },
        popupHost = {
            searchStatus.SearchPager(
                onSearchStatusChange = actions.onSearchStatusChange,
                defaultResult = {},
                emptyResult = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp)
                    ) {
                        BasicComponent(title = stringResource(R.string.accessibility_search_empty))
                    }
                },
                searchBarTopPadding = 12.dp,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .overScrollVertical(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.searchResults, key = { it.id }) { service ->
                        ServiceCard(
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
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            searchStatus.SearchBox {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = innerPadding,
                    overscrollEffect = null,
                ) {
                    if (state.error != null) {
                        item(key = "error") {
                            WarningCard(
                                message = stringResource(R.string.accessibility_toggle_failed, state.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(top = 12.dp),
                                onClick = actions.onDismissError,
                            )
                        }
                    }
                    if (!state.writable) {
                        item(key = "permission") {
                            WarningCard(
                                message = stringResource(R.string.accessibility_need_permission),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(top = 12.dp),
                                level = WarningLevel.Notice,
                                onClick = actions.onRequestShizukuPermission,
                            )
                        }
                    }
                    if (state.services.isEmpty() && !state.loading) {
                        item(key = "empty") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(top = 12.dp)
                            ) {
                                BasicComponent(
                                    title = stringResource(R.string.accessibility_empty),
                                    summary = stringResource(R.string.accessibility_empty_summary),
                                )
                            }
                        }
                    }
                    // Third-party services first; the system ones sit in a folded section underneath.
                    val userServices = state.services.filter { !it.isSystemApp }
                    val systemServices = state.services.filter { it.isSystemApp }
                    items(userServices, key = { it.id }) { service ->
                        ServiceCard(
                            service = service,
                            enabled = service.id in state.enabledIds,
                            writable = state.writable,
                            descriptionMaxLines = descriptionMaxLines,
                            expanded = service.id in state.expandedDescriptionIds,
                            onToggleDescription = { actions.onToggleDescription(service.id) },
                            onToggle = { actions.onToggle(service.id, it) },
                        )
                    }
                    if (!hideSystemApps && systemServices.isNotEmpty()) {
                        // Separated from the user apps above; one item so the rows can animate open.
                        item(key = "system_apps_gap") {
                            Spacer(Modifier.height(12.dp))
                        }
                        item(key = "system_apps_group") {
                            val chevronRotation by animateFloatAsState(
                                targetValue = if (systemAppsExpanded) 180f else 0f,
                                label = "systemAppsChevron",
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                ) {
                                    BasicComponent(
                                        title = "${stringResource(R.string.accessibility_system_apps)} (${systemServices.size})",
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Default.Android,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 8.dp),
                                            )
                                        },
                                        endActions = {
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.rotate(chevronRotation),
                                            )
                                        },
                                        onClick = { systemAppsExpanded = !systemAppsExpanded },
                                    )
                                }
                                AnimatedVisibility(visible = systemAppsExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        systemServices.forEach { service ->
                                            ServiceCard(
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
                    item {
                        Spacer(Modifier.height(bottomInnerPadding + 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: ServiceMeta,
    enabled: Boolean,
    writable: Boolean,
    descriptionMaxLines: Int,
    expanded: Boolean,
    onToggleDescription: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val description = service.description

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
    ) {
        BasicComponent(
            title = service.label,
            startAction = {
                service.icon?.let { icon ->
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp),
                    )
                }
            },
            endActions = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = writable,
                )
            },
            onClick = { onToggleDescription() },
        )
        // Upstream's semantics: the description is always visible but folded to
        // `descriptionMaxLines`, and tapping the card unfolds the rest.
        Text(
            text = description ?: stringResource(R.string.accessibility_no_description),
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = if (expanded) Int.MAX_VALUE else descriptionMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                // Indent under the title: 16.dp card padding + 52.dp icon + 8.dp gap.
                .padding(start = 76.dp, end = 16.dp, bottom = 12.dp),
        )
    }
}

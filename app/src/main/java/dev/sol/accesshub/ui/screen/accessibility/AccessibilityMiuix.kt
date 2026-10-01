package dev.sol.accesshub.ui.screen.accessibility

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sol.accesshub.R
import dev.sol.accesshub.data.ServiceMeta
import dev.sol.accesshub.ui.component.WarningLevel
import dev.sol.accesshub.ui.component.miuix.WarningCard
import dev.sol.accesshub.ui.theme.LocalEnableBlur
import dev.sol.accesshub.ui.util.BlurredBar
import dev.sol.accesshub.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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
    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                barColor = barColor,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
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
                    item {
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
                    item {
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
                    item {
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
                items(state.services, key = { it.id }) { service ->
                    ServiceCard(
                        service = service,
                        enabled = service.id in state.enabledIds,
                        writable = state.writable,
                        onToggle = { actions.onToggle(service.id, it) },
                    )
                }
                item {
                    Spacer(Modifier.height(bottomInnerPadding + 12.dp))
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    barColor: Color,
) {
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = stringResource(R.string.home_accessibility_services),
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
private fun ServiceCard(
    service: ServiceMeta,
    enabled: Boolean,
    writable: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
    ) {
        BasicComponent(
            title = service.label,
            summary = service.description ?: service.appName,
            startAction = {
                service.icon?.let { icon ->
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp),
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
            onClick = { onToggle(!enabled) },
        )
    }
}

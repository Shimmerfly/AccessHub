package dev.sol.accesshub.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sol.accesshub.R
import dev.sol.accesshub.ui.LocalMainPagerState
import dev.sol.accesshub.ui.component.FloatingBottomBar
import dev.sol.accesshub.ui.component.FloatingBottomBarItem
import dev.sol.accesshub.ui.theme.LocalEnableFloatingBottomBar
import dev.sol.accesshub.ui.theme.LocalEnableFloatingBottomBarBlur
import dev.sol.accesshub.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.label),
            icon = destination.icon,
        )
    }
    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    // NavigationBarItem is a RowScope extension, and the layout-scope DslMarker hides
                    // that receiver inside the surrounding Box, so keep it and call in explicitly.
                    val rowScope = this
                    items.forEachIndexed { index, item ->
                        // NavigationBarItem takes a plain ImageVector, so the badge is an overlay.
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            with(rowScope) {
                                NavigationBarItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = item.icon,
                                    label = item.label,
                                    selected = mainState.selectedPage == index,
                                    onClick = {
                                        mainState.animateToPage(index)
                                    }
                                )
                            }
                            badgeFor(index, navigationBadge)?.let { badge ->
                                MiuixNavBadge(
                                    badge = badge,
                                    modifier = Modifier.offset(
                                        x = NavigationBarDefaults.IconSize / 2,
                                        y = NavigationBarDefaults.IconTopPadding / 2,
                                    ),
                                )
                            }
                        }
                    }
                }
            )
        }
    } else {
        FloatingBottomBar(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            selectedIndex = { mainState.selectedPage },
            onSelected = { mainState.animateToPage(it) },
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = enableFloatingBottomBarBlur,
        ) {
            items.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = {
                        mainState.animateToPage(index)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                        badgeFor(index, navigationBadge)?.let { badge ->
                            MiuixNavBadge(
                                badge = badge,
                                modifier = Modifier.offset(x = 4.dp, y = (-4).dp),
                            )
                        }
                    }
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.home, Icons.Rounded.Cottage),
    Accessibility(R.string.accessibility_tab, Icons.Rounded.AccessibilityNew),
    Setting(R.string.settings, Icons.Rounded.Settings)
}

/**
 * Miuix 0.9.2 ships no badge component, so the badge is drawn as a small overlay and the caller
 * positions it over the item icon. Colours mirror the Material3 side.
 */
@Composable
internal fun MiuixNavBadge(
    badge: NavBadge,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (badge.tone) {
        BadgeTone.Alert -> MiuixTheme.colorScheme.error
        BadgeTone.Accent -> MiuixTheme.colorScheme.primary
    }
    val contentColor = when (badge.tone) {
        BadgeTone.Alert -> MiuixTheme.colorScheme.onError
        BadgeTone.Accent -> MiuixTheme.colorScheme.onPrimary
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = badge.count.toString(),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            color = contentColor,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            maxLines = 1,
        )
    }
}

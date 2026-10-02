package dev.sol.accesshub.ui.component.bottombar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.sol.accesshub.R
import dev.sol.accesshub.ui.LocalMainPagerState
import dev.sol.accesshub.ui.component.FloatingBottomBar
import dev.sol.accesshub.ui.component.FloatingBottomBarItem
import dev.sol.accesshub.ui.theme.LocalEnableFloatingBottomBar
import dev.sol.accesshub.ui.theme.LocalShowFloatingBottomBarLabels
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import dev.sol.accesshub.ui.theme.LocalOnSelectPage

@Composable
fun BottomBarMaterial(
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainPagerState = LocalMainPagerState.current
    val onSelectPage = LocalOnSelectPage.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current

    val items = listOf(
        Triple(R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
        Triple(
            R.string.accessibility_tab,
            Icons.Filled.AccessibilityNew,
            Icons.Outlined.AccessibilityNew
        ),
        Triple(R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    if (enableFloatingBottomBar) {
        // The miuix floating bar without its liquid glass layer: the opaque container also drops the
        // backdrop's edge fade. It still takes a backdrop because the component asks for one, but
        // with isBlurEnabled = false nothing is drawn from it.
        val backdrop = rememberLayerBackdrop()
        val showLabels = LocalShowFloatingBottomBarLabels.current
        // The panel sizes itself to its content and sits at CenterStart, so the host has to centre
        // it: own the full width here and let the box do the centring.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            contentAlignment = Alignment.Center,
        ) {
            FloatingBottomBar(
            modifier = modifier,
            selectedIndex = { mainPagerState.selectedPage },
            onSelected = { onSelectPage(it) },
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = false,
        ) {
            items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
                FloatingBottomBarItem(
                    onClick = {
                        if (mainPagerState.selectedPage != index) {
                            onSelectPage(index)
                        }
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NavigationIconWithBadge(
                            icon = if (mainPagerState.selectedPage == index) selectedIcon else unselectedIcon,
                            contentDescription = stringResource(label),
                            badge = badgeFor(index, navigationBadge),
                        )
                        if (showLabels) {
                            Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            }
        }
        return
    }

    FlexibleBottomAppBar(
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) {
        items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
            val selected = mainPagerState.selectedPage == index
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onSelectPage(index)
                    }
                },
                icon = {
                    NavigationIconWithBadge(
                        icon = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = stringResource(label),
                        badge = badgeFor(index, navigationBadge),
                    )
                },
                label = {
                    Text(
                        stringResource(label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

/** Icon with the destination's badge, shared by the Material bar and rail. */
@Composable
internal fun NavigationIconWithBadge(
    icon: ImageVector,
    contentDescription: String?,
    badge: NavBadge?,
) {
    if (badge != null) {
        BadgedBox(
            badge = {
                when (badge.tone) {
                    BadgeTone.Alert -> Badge {
                        Text(badge.count.toString())
                    }

                    BadgeTone.Accent -> Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            }
        ) {
            Icon(icon, contentDescription)
        }
    } else {
        Icon(icon, contentDescription)
    }
}

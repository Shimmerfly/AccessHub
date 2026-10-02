package dev.sol.accesshub.ui.component.bottombar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sol.accesshub.ui.LocalMainPagerState
import dev.sol.accesshub.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.sol.accesshub.ui.theme.LocalOnSelectPage

@Composable
fun NavigationRailMiuix(
    blurBackdrop: LayerBackdrop?,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val onSelectPage = LocalOnSelectPage.current

    val items = BottomBarDestination.entries.map { destination ->
        Pair(stringResource(destination.label), destination.icon)
    }

    BlurredBar(blurBackdrop) {
        NavigationRail(
            modifier = modifier
                .fillMaxHeight(),
            color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            items.forEachIndexed { index, (label, icon) ->
                // NavigationRailItem takes a plain ImageVector, so the badge is an overlay.
                Box(
                    modifier = Modifier.padding(vertical = 4.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavigationRailItem(
                        icon = icon,
                        label = label,
                        selected = mainState.selectedPage == index,
                        onClick = {
                            onSelectPage(index)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    badgeFor(index, navigationBadge)?.let { badge ->
                        MiuixNavBadge(
                            badge = badge,
                            modifier = Modifier.offset(
                                x = NavigationRailDefaults.IconSize / 2,
                                y = NavigationRailDefaults.ItemVerticalPadding / 2,
                            ),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

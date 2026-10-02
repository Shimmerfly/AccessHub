package dev.sol.accesshub.ui.component.bottombar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sol.accesshub.R
import dev.sol.accesshub.ui.LocalMainPagerState
import dev.sol.accesshub.ui.theme.LocalOnSelectPage

@Composable
fun NavigationRailMaterial(
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainPagerState = LocalMainPagerState.current
    val onSelectPage = LocalOnSelectPage.current

    val items = listOf(
        Triple(R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
        Triple(
            R.string.accessibility_tab,
            Icons.Filled.AccessibilityNew,
            Icons.Outlined.AccessibilityNew
        ),
        Triple(R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Start + WindowInsetsSides.Vertical
        )
    ) {
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
            val selected = mainPagerState.selectedPage == index
            NavigationRailItem(
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
                label = { Text(stringResource(label)) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

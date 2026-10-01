package dev.sol.accesshub.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sol.accesshub.R
import dev.sol.accesshub.shizuku.PrivilegedProvider
import dev.sol.accesshub.shizuku.ShizukuState
import dev.sol.accesshub.ui.component.WarningLevel
import dev.sol.accesshub.ui.component.dialog.rememberConfirmDialog
import dev.sol.accesshub.ui.component.material.SegmentedColumn
import dev.sol.accesshub.ui.component.material.SegmentedListItem
import dev.sol.accesshub.ui.component.material.TonalCard

@Composable
fun HomePagerMaterial(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = { TopBar(scrollBehavior = scrollBehavior) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            if (state.checkUpdateEnabled) {
                UpdateCard(state = state, actions = actions)
            }
            StatusCard(state = state, actions = actions)
            InfoCard(state = state)
            SupportLinks(actions = actions)
            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}

@Composable
private fun UpdateCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val newVersion = state.latestVersionInfo

    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { actions.onOpenUrl(newVersion.downloadUrl) })
        val updateTitle = stringResource(R.string.home_update_dialog_title)
        val updateConfirm = stringResource(R.string.home_update_confirm)

        WarningCard(
            message = stringResource(R.string.home_update_available, newVersion.versionCode),
            level = WarningLevel.Notice,
            onClick = {
                if (newVersion.changelog.isEmpty()) {
                    actions.onOpenUrl(newVersion.downloadUrl)
                } else {
                    updateDialog.showConfirm(
                        title = updateTitle,
                        content = newVersion.changelog,
                        markdown = true,
                        confirm = updateConfirm,
                    )
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
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

/**
 * Hero card of the page, mirroring upstream's kernel status card: it turns green once Shizuku
 * answers, otherwise it becomes a tappable row that walks the user towards a working setup.
 */
@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val ready = state.shizuku.isReady
    val provider = state.shizuku.provider
    val providerName = provider.displayName.ifEmpty { stringResource(R.string.home_shizuku_or_sui) }
    val shizukuUrl = stringResource(R.string.home_shizuku_url)
    val suiUrl = stringResource(R.string.home_sui_url)
    val containerColor =
        if (ready) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)

    val title: String
    val summary: String
    when (state.shizuku.state) {
        ShizukuState.READY -> {
            title = stringResource(R.string.home_provider_ready, providerName)
            summary = stringResource(
                R.string.home_shizuku_ready_summary,
                state.shizuku.version,
                state.shizuku.uid,
            )
        }

        ShizukuState.NO_PERMISSION -> {
            title = stringResource(R.string.home_shizuku_no_permission, providerName)
            summary = stringResource(R.string.home_shizuku_no_permission_summary, providerName)
        }

        ShizukuState.NOT_RUNNING -> {
            if (provider == PrivilegedProvider.SUI) {
                title = stringResource(R.string.home_sui_not_running)
                summary = stringResource(R.string.home_sui_not_running_summary)
            } else {
                title = stringResource(R.string.home_shizuku_not_running)
                summary = stringResource(R.string.home_shizuku_not_running_summary)
            }
        }

        ShizukuState.NOT_INSTALLED -> {
            title = stringResource(R.string.home_shizuku_not_installed)
            summary = stringResource(R.string.home_shizuku_not_installed_summary)
        }

        ShizukuState.UNKNOWN -> {
            title = stringResource(R.string.home_shizuku_detecting)
            summary = stringResource(R.string.home_shizuku_detecting_summary)
        }
    }
    val onClick: (() -> Unit)? = when (state.shizuku.state) {
        ShizukuState.READY -> null
        ShizukuState.NO_PERMISSION -> actions.onRequestShizukuPermission
        ShizukuState.NOT_RUNNING -> if (provider == PrivilegedProvider.SUI) {
            ({ actions.onOpenUrl(suiUrl) })
        } else {
            actions.onOpenShizuku
        }
        ShizukuState.NOT_INSTALLED -> ({ actions.onOpenUrl(shizukuUrl) })
        ShizukuState.UNKNOWN -> null
    }

    val cardContent = @Composable {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = title,
                )
            },
            headlineContent = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(text = summary, style = MaterialTheme.typography.bodyMedium) },
            trailingContent = if (ready) {
                {
                    Text(
                        text = stringResource(
                            if (state.shizuku.isRoot) R.string.home_shizuku_mode_root
                            else R.string.home_shizuku_mode_adb
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = contentColor,
                leadingContentColor = contentColor,
                trailingContentColor = contentColor,
                supportingContentColor = contentColor.copy(alpha = 0.7f)
            ),
        )
    }

    if (onClick != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            onClick = onClick,
        ) {
            cardContent()
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
        ) {
            cardContent()
        }
    }
}

@Composable
private fun InfoCard(state: HomeUiState) {
    @Composable
    fun InfoCardItem(
        icon: ImageVector,
        label: String,
        content: String,
    ) {
        SegmentedListItem(
            headlineContent = { Text(text = label, style = MaterialTheme.typography.bodyLarge) },
            leadingContent = { Icon(imageVector = icon, contentDescription = label) },
            supportingContent = {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }

    val shizukuContent = if (state.shizuku.isRunning) {
        stringResource(R.string.home_shizuku_value, state.shizuku.version, state.shizuku.uid)
    } else {
        stringResource(R.string.home_shizuku_unavailable)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        SegmentedColumn(
            modifier = Modifier.fillMaxWidth(),
            content = listOf(
                {
                    InfoCardItem(
                        icon = Icons.Filled.Tag,
                        label = stringResource(R.string.home_app_version),
                        content = state.systemInfo.appVersion,
                    )
                },
                {
                    InfoCardItem(
                        icon = Icons.Filled.Android,
                        label = stringResource(R.string.home_android_version),
                        content = state.systemInfo.androidVersion,
                    )
                },
                {
                    InfoCardItem(
                        icon = Icons.Filled.Smartphone,
                        label = stringResource(R.string.home_device_model),
                        content = state.systemInfo.deviceModel,
                    )
                },
                {
                    InfoCardItem(
                        icon = Icons.Filled.Fingerprint,
                        label = stringResource(R.string.home_fingerprint),
                        content = state.systemInfo.fingerprint,
                    )
                },
            )
        )
        SegmentedColumn(
            modifier = Modifier.fillMaxWidth(),
            content = listOf(
                {
                    InfoCardItem(
                        icon = Icons.Filled.Accessibility,
                        label = stringResource(R.string.home_accessibility_services),
                        content = stringResource(
                            R.string.home_accessibility_services_value,
                            state.accessibility.enabledCount,
                            state.accessibility.installedCount,
                        ),
                    )
                },
                {
                    InfoCardItem(
                        icon = Icons.Filled.Code,
                        label = state.shizuku.provider.displayName.ifEmpty {
                            stringResource(R.string.home_shizuku_or_sui)
                        },
                        content = shizukuContent,
                    )
                },
            )
        )
    }
}

@Composable
private fun SupportLinks(actions: HomeActions) {
    val repoUrl = stringResource(R.string.home_repo_url)
    val shizukuUrl = stringResource(R.string.home_shizuku_url)

    SegmentedColumn(
        modifier = Modifier.fillMaxWidth(),
        content = listOf(
            {
                SegmentedListItem(
                    onClick = { actions.onOpenUrl(repoUrl) },
                    headlineContent = { Text(stringResource(R.string.home_link_repo_title)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.home_link_repo_summary,
                                stringResource(R.string.app_name)
                            )
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Code, stringResource(R.string.home_link_repo_title)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
            },
            {
                SegmentedListItem(
                    onClick = { actions.onOpenUrl(shizukuUrl) },
                    headlineContent = { Text(stringResource(R.string.home_link_shizuku_title)) },
                    supportingContent = { Text(stringResource(R.string.home_link_shizuku_summary)) },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            stringResource(R.string.home_link_shizuku_title)
                        )
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
            },
            {
                SegmentedListItem(
                    onClick = actions.onPermissionsClick,
                    headlineContent = { Text(stringResource(R.string.home_link_permissions_title)) },
                    supportingContent = { Text(stringResource(R.string.home_link_permissions_summary)) },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Security,
                            stringResource(R.string.home_link_permissions_title)
                        )
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
            },
        )
    )
}

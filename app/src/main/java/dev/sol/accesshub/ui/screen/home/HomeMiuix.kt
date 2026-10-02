package dev.sol.accesshub.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sol.accesshub.R
import dev.sol.accesshub.shizuku.PrivilegedProvider
import dev.sol.accesshub.shizuku.ShizukuState
import dev.sol.accesshub.ui.component.WarningLevel
import dev.sol.accesshub.ui.component.dialog.rememberConfirmDialog
import dev.sol.accesshub.ui.component.miuix.WarningCard
import dev.sol.accesshub.ui.theme.LocalEnableBlur
import dev.sol.accesshub.ui.theme.isInDarkTheme
import dev.sol.accesshub.ui.util.BlurredBar
import dev.sol.accesshub.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import dev.sol.accesshub.ui.LocalMainPagerState

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
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
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.checkUpdateEnabled) {
                            UpdateCard(state = state, actions = actions)
                        }
                        StatusCard(state = state, actions = actions)
                        InfoCard(state = state, actions = actions)
                        SupportLinks(actions = actions)
                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
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
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    barColor: Color,
) {
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = stringResource(R.string.app_name),
            scrollBehavior = scrollBehavior
        )
    }
}

/**
 * Hero card of the page, mirroring upstream's kernel status card: green while Shizuku answers,
 * otherwise an actionable row that walks the user towards a working setup.
 */
@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val provider = state.shizuku.provider
    val providerName = provider.displayName.ifEmpty { stringResource(R.string.home_shizuku_or_sui) }
    val shizukuUrl = stringResource(R.string.home_shizuku_url)
    val suiUrl = stringResource(R.string.home_sui_url)

    when (state.shizuku.state) {
        ShizukuState.READY -> ReadyStatusCard(state = state)

        ShizukuState.NO_PERMISSION -> ActionStatusCard(
            title = stringResource(R.string.home_shizuku_no_permission, providerName),
            summary = stringResource(R.string.home_shizuku_no_permission_summary, providerName),
            onClick = actions.onRequestShizukuPermission,
        )

        ShizukuState.NOT_RUNNING -> if (provider == PrivilegedProvider.SUI) {
            ActionStatusCard(
                title = stringResource(R.string.home_sui_not_running),
                summary = stringResource(R.string.home_sui_not_running_summary),
                onClick = { actions.onOpenUrl(suiUrl) },
            )
        } else {
            ActionStatusCard(
                title = stringResource(R.string.home_shizuku_not_running),
                summary = stringResource(R.string.home_shizuku_not_running_summary),
                onClick = actions.onOpenShizuku,
            )
        }

        ShizukuState.NOT_INSTALLED -> ActionStatusCard(
            title = stringResource(R.string.home_shizuku_not_installed),
            summary = stringResource(R.string.home_shizuku_not_installed_summary),
            onClick = { actions.onOpenUrl(shizukuUrl) },
        )

        ShizukuState.UNKNOWN -> ActionStatusCard(
            title = stringResource(R.string.home_shizuku_detecting),
            summary = stringResource(R.string.home_shizuku_detecting_summary),
            onClick = null,
        )
    }
}

@Composable
private fun ReadyStatusCard(state: HomeUiState) {
    val containerColor = when {
        isDynamicColor -> colorScheme.secondaryContainer
        isInDarkTheme() -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val iconColor = if (isDynamicColor) colorScheme.primary.copy(alpha = 0.8f) else Color(0xFF36D167)
    val textColor = if (isDynamicColor) colorScheme.onSecondaryContainer else colorScheme.onSurface
    val providerName = state.shizuku.provider.displayName.ifEmpty { stringResource(R.string.home_shizuku_or_sui) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = containerColor),
        showIndication = false,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 70.dp, y = 44.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(182.dp),
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    tint = iconColor,
                    contentDescription = null,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 26.dp, end = 148.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_provider_ready, providerName),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        // The version moved out of the summary and became the API badge below.
                        text = stringResource(R.string.home_shizuku_ready_uid, state.shizuku.uid),
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = 0.72f),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 24.dp, end = 148.dp, bottom = 20.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                ApiBadgeMiuix(
                    text = stringResource(R.string.home_shizuku_api_badge, state.shizuku.version),
                )
            }
        }
    }
}

@Composable
private fun ActionStatusCard(
    title: String,
    summary: String,
    onClick: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        BasicComponent(
            title = title,
            summary = summary,
            startAction = {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = title,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
        )
    }
}

@Composable
private fun InfoCard(state: HomeUiState, actions: HomeActions) {
    @Composable
    fun InfoText(
        icon: ImageVector,
        title: String,
        content: String,
        bottomPadding: Dp = 24.dp,
        onClick: (() -> Unit)? = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = colorScheme.onSurface,
            )
            Column {
                Text(
                    text = title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = content,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }

    val mainPagerState = LocalMainPagerState.current
    val suiUrl = stringResource(R.string.home_sui_url)

    val shizukuContent = if (state.shizuku.isRunning) {
        stringResource(R.string.home_shizuku_value, state.shizuku.version, state.shizuku.uid) +
            if (state.shizuku.isRoot) " · " + stringResource(R.string.home_shizuku_mode_root) else ""
    } else {
        stringResource(R.string.home_shizuku_unavailable)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(
                    icon = Icons.Filled.Tag,
                    title = stringResource(R.string.home_app_version),
                    content = state.systemInfo.appVersion,
                )
                InfoText(
                    icon = Icons.Filled.Android,
                    title = stringResource(R.string.home_android_version),
                    content = state.systemInfo.androidVersion,
                )
                InfoText(
                    icon = Icons.Filled.Smartphone,
                    title = stringResource(R.string.home_device_model),
                    content = state.systemInfo.deviceModel,
                )
                InfoText(
                    icon = Icons.Filled.Code,
                    title = state.shizuku.provider.displayName.ifEmpty {
                        stringResource(R.string.home_shizuku_or_sui)
                    },
                    content = shizukuContent,
                    bottomPadding = 0.dp,
                    onClick = {
                        if (state.shizuku.provider == PrivilegedProvider.SUI) {
                            actions.onOpenUrl(suiUrl)
                        } else {
                            actions.onOpenShizuku()
                        }
                    },
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(
                    icon = Icons.Filled.Accessibility,
                    title = stringResource(R.string.home_accessibility_services),
                    content = stringResource(
                        R.string.home_accessibility_services_value,
                        state.accessibility.enabledCount,
                        state.accessibility.installedCount,
                    ),
                    bottomPadding = 0.dp,
                    onClick = { mainPagerState.animateToPage(1) },
                )
            }
        }
    }
}

@Composable
private fun SupportLinks(actions: HomeActions) {
    val repoUrl = stringResource(R.string.home_repo_url)
    val shizukuUrl = stringResource(R.string.home_shizuku_url)

    Card(modifier = Modifier.fillMaxWidth()) {
        ArrowPreference(
            title = stringResource(R.string.home_link_repo_title),
            summary = stringResource(R.string.home_link_repo_summary, stringResource(R.string.app_name)),
            startAction = {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = stringResource(R.string.home_link_repo_title),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { actions.onOpenUrl(repoUrl) },
            holdDownState = false,
            enabled = true,
        )
        ArrowPreference(
            title = stringResource(R.string.home_link_shizuku_title),
            summary = stringResource(R.string.home_link_shizuku_summary),
            startAction = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.home_link_shizuku_title),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { actions.onOpenUrl(shizukuUrl) },
            holdDownState = false,
            enabled = true,
        )
    }
}

/** Small pill mirroring LSPosed's API badge, under the status card's summary line. */
@Composable
private fun ApiBadgeMiuix(text: String) {
    // Bright Monet container: tertiaryContainer is the palette's soft accent, unlike the red
    // errorContainer or the plain primaryContainer.
    val colorScheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.tertiaryContainer),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

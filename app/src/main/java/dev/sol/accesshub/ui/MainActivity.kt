package dev.sol.accesshub.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.MutableStateFlow
import dev.sol.accesshub.R
import dev.sol.accesshub.accessHubApp
import dev.sol.accesshub.data.repository.AccessibilityRepository
import dev.sol.accesshub.ui.component.bottombar.BottomBar
import dev.sol.accesshub.ui.component.bottombar.MainPagerState
import dev.sol.accesshub.ui.component.bottombar.NavigationBadgeState
import dev.sol.accesshub.ui.component.bottombar.SideRail
import dev.sol.accesshub.ui.component.bottombar.rememberMainPagerState
import dev.sol.accesshub.ui.navigation3.LocalNavigator
import dev.sol.accesshub.ui.navigation3.Navigator
import dev.sol.accesshub.ui.navigation3.Route
import dev.sol.accesshub.ui.navigation3.rememberNavigator
import dev.sol.accesshub.ui.screen.about.AboutScreen
import dev.sol.accesshub.ui.screen.accessibility.AccessibilityPager
import dev.sol.accesshub.ui.screen.colorpalette.ColorPaletteScreen
import dev.sol.accesshub.ui.screen.home.HomePager
import dev.sol.accesshub.ui.screen.settings.SettingPager
import dev.sol.accesshub.ui.theme.AccessHubTheme
import dev.sol.accesshub.ui.theme.LocalColorMode
import dev.sol.accesshub.ui.theme.LocalEnableBlur
import dev.sol.accesshub.ui.theme.LocalEnableFloatingBottomBar
import dev.sol.accesshub.ui.theme.LocalEnableFloatingBottomBarBlur
import dev.sol.accesshub.ui.theme.LocalShowFloatingBottomBarLabels
import dev.sol.accesshub.ui.theme.LocalEnableNavigationBadge
import dev.sol.accesshub.ui.util.rememberBlurBackdrop
import dev.sol.accesshub.ui.util.rememberContentReady
import dev.sol.accesshub.ui.viewmodel.MainActivityViewModel
import dev.sol.accesshub.ui.viewmodel.MainPagerConfig
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.sol.accesshub.data.repository.EnabledServicesCounter
import androidx.compose.ui.res.stringResource
import dev.sol.accesshub.shizuku.ShizukuManager
import dev.sol.accesshub.shizuku.ShizukuState
import dev.sol.accesshub.ui.component.dialog.rememberConfirmDialog
import dev.sol.accesshub.ui.theme.LocalOnSelectPage

class MainActivity : ComponentActivity() {

    private val intentState = MutableStateFlow(0)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val darkMode = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            val navigator = rememberNavigator(Route.Main)
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableNavigationBadge provides uiState.enableNavigationBadge,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
                LocalShowFloatingBottomBarLabels provides uiState.showFloatingBottomBarLabels,
                LocalUiMode provides uiMode,
            ) {
                AccessHubTheme(appSettings = appSettings, uiMode = uiMode) {
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }

                    val navDisplay = @Composable {
                        NavDisplay(
                            backStack = navigator.backStack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            onBack = {
                                navigator.pop()
                            },
                            entryProvider = entryProvider {
                                entry<Route.Main> { mainScreenEntry() }
                                entry<Route.About> { AboutScreen() }
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                entry<Route.Home> { mainScreenEntry() }
                                entry<Route.Settings> { mainScreenEntry() }
                            }
                        )
                    }

                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold { navDisplay() }
                        UiMode.Miuix -> Scaffold { navDisplay() }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Increment intentState to trigger LaunchedEffect re-execution
        intentState.value += 1
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableNavigationBadge = LocalEnableNavigationBadge.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MainPagerConfig.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)

    // The services page cannot switch accessibility services without Shizuku, so tapping its tab
    // asks for the permission instead of moving there.
    val shizukuStatus by ShizukuManager.status.collectAsStateWithLifecycle()
    val needsShizukuPermission = shizukuStatus.state == ShizukuState.NO_PERMISSION
    val permissionTitle = stringResource(R.string.accessibility_shizuku_permission_title)
    val permissionMessage = stringResource(R.string.accessibility_shizuku_permission_message)
    val permissionGrant = stringResource(R.string.accessibility_shizuku_permission_grant)
    val permissionDialog = rememberConfirmDialog(
        onConfirm = { ShizukuManager.requestPermission() },
    )
    val selectPage: (Int) -> Unit = { index ->
        if (index == SERVICES_PAGE && needsShizukuPermission) {
            permissionDialog.showConfirm(
                title = permissionTitle,
                content = permissionMessage,
                markdown = false,
                html = false,
                confirm = permissionGrant,
                dismiss = null,
                isWarning = true,
            )
        } else {
            mainPagerState.animateToPage(index)
        }
    }
    var userScrollEnabled by remember { mutableStateOf(true) }
    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface // Blur is not used in Material, this is just a placeholder
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    val blurBackdrop = rememberBlurBackdrop(enableBlur)

    // The enabled set is a cheap Settings.Secure read, but it changes outside the app (and from the
    // Services page), so read it whenever this screen enters the composition or the page changes.
    // A resume effect alone is not enough: when the theme toggle brings this screen back, the
    // composition starts after the host is already resumed, so that ON_RESUME never arrives.
    // One shared counter: this screen publishes what it reads, the services page publishes what it
    // toggles, and the badge just shows whatever was published last.
    val publishedCount by EnabledServicesCounter.count.collectAsStateWithLifecycle()
    LaunchedEffect(enableNavigationBadge, mainPagerState.pagerState.settledPage) {
        EnabledServicesCounter.publish(
            if (enableNavigationBadge) AccessibilityRepository(accessHubApp).readEnabled().services.size else 0
        )
    }
    LifecycleResumeEffect(enableNavigationBadge) {
        EnabledServicesCounter.publish(
            if (enableNavigationBadge) AccessibilityRepository(accessHubApp).readEnabled().services.size else 0
        )
        onPauseOrDispose { }
    }
    val navigationBadge = NavigationBadgeState(
        enabledServiceCount = if (enableNavigationBadge) publishedCount ?: 0 else 0
    )

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // A floating bar replaces the rail in both themes, so the bar is never hidden behind it.
    val useNavigationRail = isLandscape && !enableFloatingBottomBar

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalOnSelectPage provides selectPage,
    ) {
        val contentReady = rememberContentReady()
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    modifier = Modifier
                        .then(if (enableFloatingBottomBar && enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 1 else 0,
                    userScrollEnabled = userScrollEnabled,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (isCurrentPage || contentReady) HomePager(navController, bottomInnerPadding, isCurrentPage)
                        1 -> if (isCurrentPage || contentReady) AccessibilityPager(bottomInnerPadding, isCurrentPage)
                        2 -> if (isCurrentPage || contentReady) SettingPager(navController, bottomInnerPadding)
                    }
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold {
                    Row {
                        SideRail(
                            blurBackdrop = blurBackdrop,
                            navigationBadge = navigationBadge,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold { _ ->
                    Row {
                        SideRail(
                            blurBackdrop = blurBackdrop,
                            navigationBadge = navigationBadge,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        navigationBadge = navigationBadge,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}

/** Index of the services page inside the main pager. */
private const val SERVICES_PAGE = 1

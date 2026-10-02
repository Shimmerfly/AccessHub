package dev.sol.accesshub.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import dev.sol.accesshub.BuildConfig
import dev.sol.accesshub.R
import dev.sol.accesshub.ui.LocalUiMode
import dev.sol.accesshub.ui.UiMode
import dev.sol.accesshub.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val title = stringResource(R.string.about)
    val appName = stringResource(R.string.app_name)
    val htmlString = stringResource(
        id = R.string.about_source_link,
        "<b><a href=\"https://github.com/Shimmerfly/AccessHub\">Github</a></b>"
    )
    // Parse once per string, not once per recomposition: the state must also stay the same instance
    // so the page below can skip recomposition instead of redoing the whole list every frame.
    val state = remember(title, appName, htmlString) {
        AboutUiState(
            title = title,
            appName = appName,
            versionName = BuildConfig.VERSION_NAME,
            links = extractLinks(htmlString),
        )
    }
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = uriHandler::openUri,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}

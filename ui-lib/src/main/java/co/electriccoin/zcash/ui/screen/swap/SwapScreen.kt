package co.electriccoin.zcash.ui.screen.swap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.appbar.ZashiTopAppBarVM
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.LocalNavController
import co.electriccoin.zcash.ui.design.util.tryRequestFocus
import co.electriccoin.zcash.ui.screen.swap.upi.UpiOfframpBody
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SwapScreen() {
    val navigationRouter = koinInject<NavigationRouter>()
    var selectedTab by rememberSaveable { mutableStateOf(SwapTab.SWAP) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ZappTheme.colors.bg)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
    ) {
        SwapTabsToolbar(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            onBack = { navigationRouter.back() },
        )
        when (selectedTab) {
            SwapTab.SWAP -> SwapBody()
            SwapTab.OFFRAMP -> UpiOfframpBody()
        }
    }
}

@Composable
private fun SwapBody() {
    val vm = koinViewModel<SwapVM>()
    val appBarVM = koinViewModel<ZashiTopAppBarVM>()
    val state by vm.state.collectAsStateWithLifecycle()
    val cancelState by vm.cancelState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    var hasBeenAutofocused by rememberSaveable {
        val isSwapFirstScreen =
            navController
                .currentBackStackEntry
                ?.destination
                ?.route == SwapArgs::class.qualifiedName
        mutableStateOf(!isSwapFirstScreen)
    }
    val appBarState by appBarVM.state.collectAsStateWithLifecycle()
    state?.let {
        SwapView(
            state = it,
            appBarState = appBarState,
            onSideEffect = { amountFocusRequester ->
                if (!hasBeenAutofocused) {
                    hasBeenAutofocused = amountFocusRequester.tryRequestFocus() ?: true
                }
            },
            embeddedInTabHost = true,
        )
    }
    BackHandler(state != null) { state?.onBack?.invoke() }
    SwapCancelView(cancelState)
}

@Serializable
data object SwapArgs

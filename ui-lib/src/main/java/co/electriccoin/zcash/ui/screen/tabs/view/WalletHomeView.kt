package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappSectionLabel
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar
import co.electriccoin.zcash.ui.screen.balances.BalanceWidgetArgs
import co.electriccoin.zcash.ui.screen.balances.BalanceWidgetVM
import co.electriccoin.zcash.ui.screen.home.HomeVM
import co.electriccoin.zcash.ui.screen.home.balancechart.BalanceChartVM
import co.electriccoin.zcash.ui.screen.tabs.viewmodel.WalletSyncStateVM
import co.electriccoin.zcash.ui.screen.transactionhistory.widget.ActivityWidgetVM
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun WalletHomeView() {
    val balanceVM: BalanceWidgetVM =
        koinViewModel {
            parametersOf(
                BalanceWidgetArgs(
                    isBalanceButtonEnabled = false,
                    isExchangeRateButtonEnabled = true,
                    showDust = false,
                ),
            )
        }
    val homeVM: HomeVM = koinViewModel()
    val activityVM: ActivityWidgetVM = koinViewModel()
    val chartVM: BalanceChartVM = koinViewModel()
    val syncVM: WalletSyncStateVM = koinViewModel()

    val balanceState by balanceVM.state.collectAsStateWithLifecycle()
    val homeState by homeVM.state.collectAsStateWithLifecycle()
    // Side-effect-only subscription. The pipeline drives sync-error + restore-success
    // navigation inside HomeVM; collecting here keeps its WhileSubscribed scope alive
    // for as long as this screen is on. Same pattern as AndroidHome.kt:37.
    homeVM.uiLifecyclePipeline.collectAsStateWithLifecycle()
    val activityState by activityVM.state.collectAsStateWithLifecycle()
    val chartState by chartVM.state.collectAsStateWithLifecycle()
    val syncChip by syncVM.state.collectAsStateWithLifecycle()

    val c = ZappTheme.colors

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = navBarBottom + ZappNavBar.CLEARANCE_DP.dp),
        ) {
            item {
                ZappScreenHeader(
                    title = stringResource(R.string.home_wallet_title),
                    right = { SyncStatusChip(state = syncChip) },
                )
            }

            item { SyncProgressRow(state = syncChip) }

            item {
                Spacer(Modifier.height(14.dp))
                BalanceCard(
                    balanceState = balanceState,
                    chartState = chartState,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                ZappSectionLabel(
                    text = stringResource(R.string.home_recent_activity_title),
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                )
            }

            activitySection(activityState)
        }

        WalletActionFabStack(
            onSend = { homeState?.secondButton?.onClick?.invoke() },
            onReceive = { homeState?.firstButton?.onClick?.invoke() },
            onSwap = { homeState?.fourthButton?.onClick?.invoke() },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

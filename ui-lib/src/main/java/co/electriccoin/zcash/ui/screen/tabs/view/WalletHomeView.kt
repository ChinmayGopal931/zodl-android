package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cash.z.ecc.android.sdk.model.Zatoshi
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.wallet.ExchangeRateState
import co.electriccoin.zcash.ui.design.component.chart.SparkChart
import co.electriccoin.zcash.ui.design.component.chart.SparkChartData
import co.electriccoin.zcash.ui.design.component.zapp.ZappChipVariant
import co.electriccoin.zcash.ui.design.component.zapp.ZappFab
import co.electriccoin.zcash.ui.design.component.zapp.ZappRowDivider
import co.electriccoin.zcash.ui.design.component.zapp.ZappScreenHeader
import co.electriccoin.zcash.ui.design.component.zapp.ZappSectionLabel
import co.electriccoin.zcash.ui.design.component.zapp.ZappSegmentedSelector
import co.electriccoin.zcash.ui.design.component.zapp.ZappStatusChip
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import androidx.compose.ui.platform.LocalContext
import co.electriccoin.zcash.ui.design.theme.colors.ZappNavBar
import co.electriccoin.zcash.ui.design.util.TickerLocation
import co.electriccoin.zcash.ui.design.util.getString
import co.electriccoin.zcash.ui.design.util.getValue
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.balances.BalanceWidgetArgs
import co.electriccoin.zcash.ui.screen.balances.BalanceWidgetState
import co.electriccoin.zcash.ui.screen.balances.BalanceWidgetVM
import co.electriccoin.zcash.ui.screen.home.HomeVM
import co.electriccoin.zcash.ui.screen.home.balancechart.BalanceChartPeriod
import co.electriccoin.zcash.ui.screen.home.balancechart.BalanceChartState
import co.electriccoin.zcash.ui.screen.home.balancechart.BalanceChartVM
import co.electriccoin.zcash.ui.screen.tabs.viewmodel.WalletSyncChipState
import co.electriccoin.zcash.ui.screen.tabs.viewmodel.WalletSyncStateVM
import co.electriccoin.zcash.ui.screen.tabs.viewmodel.WalletSyncStatus
import co.electriccoin.zcash.ui.screen.transactionhistory.ActivityState
import co.electriccoin.zcash.ui.screen.transactionhistory.widget.ActivityWidgetState
import co.electriccoin.zcash.ui.screen.transactionhistory.widget.ActivityWidgetVM
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.absoluteValue

@Composable
fun WalletHomeView() {
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
        modifier = Modifier
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
                    title = "Wallet",
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
                    text = "Recent activity",
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

@Composable
private fun WalletActionFabStack(
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                end = 18.dp,
                bottom = ZappNavBar.FAB_BOTTOM_PADDING_DP.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        ZappFab(
            icon = Icons.AutoMirrored.Filled.CallMade,
            contentDescription = "Send",
            onClick = onSend,
        )
        ZappFab(
            icon = Icons.AutoMirrored.Filled.CallReceived,
            contentDescription = "Receive",
            onClick = onReceive,
        )
        ZappFab(
            icon = Icons.Default.SwapHoriz,
            contentDescription = "Swap",
            onClick = onSwap,
        )
    }
}

@Composable
private fun SyncProgressRow(state: WalletSyncChipState) {
    val label = when (state.status) {
        WalletSyncStatus.SYNCING -> "Syncing"
        WalletSyncStatus.RESTORING -> "Restoring"
        WalletSyncStatus.INITIALIZING -> "Connecting"
        WalletSyncStatus.DISCONNECTED -> "Offline — reconnecting"
        WalletSyncStatus.ERROR -> "Sync error"
        WalletSyncStatus.SYNCED -> return
    }
    val c = ZappTheme.colors
    val isError = state.status == WalletSyncStatus.DISCONNECTED || state.status == WalletSyncStatus.ERROR
    val fillColor = if (isError) c.danger else c.accent
    val fraction = (state.progressPercent.coerceIn(0, 100)) / 100f
    val showPercent = state.status == WalletSyncStatus.SYNCING || state.status == WalletSyncStatus.RESTORING

    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = label,
                style = ZappTheme.typography.rowSubtitle.copy(
                    color = if (isError) c.danger else c.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
            )
            if (showPercent) {
                BasicText(
                    text = "${state.progressPercent}%",
                    style = ZappTheme.typography.rowSubtitle.copy(
                        color = c.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(c.surfaceAlt, RectangleShape),
        ) {
            if (showPercent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                        .height(3.dp)
                        .background(fillColor, RectangleShape),
                )
            } else if (isError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(fillColor, RectangleShape),
                )
            }
        }
    }
}

@Composable
private fun SyncStatusChip(state: WalletSyncChipState) {
    val c = ZappTheme.colors
    when (state.status) {
        WalletSyncStatus.SYNCED ->
            ZappStatusChip("Synced", variant = ZappChipVariant.Success, dotColor = c.success)
        WalletSyncStatus.SYNCING ->
            ZappStatusChip("Syncing ${state.progressPercent}%", variant = ZappChipVariant.Accent, dotColor = c.accent)
        WalletSyncStatus.RESTORING ->
            ZappStatusChip("Restoring ${state.progressPercent}%", variant = ZappChipVariant.Accent, dotColor = c.accent)
        WalletSyncStatus.DISCONNECTED ->
            ZappStatusChip("Offline", variant = ZappChipVariant.Danger, dotColor = c.danger)
        WalletSyncStatus.ERROR ->
            ZappStatusChip("Sync error", variant = ZappChipVariant.Danger, dotColor = c.danger)
        WalletSyncStatus.INITIALIZING ->
            ZappStatusChip("Connecting", variant = ZappChipVariant.Muted, dotColor = c.textSubtle)
    }
}

@Composable
private fun BalanceCard(
    balanceState: BalanceWidgetState,
    chartState: BalanceChartState,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    val hasBalance = balanceState.totalBalance.value > 0L

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        ZappSectionLabel(text = "Total balance")
        Spacer(Modifier.height(8.dp))

        BalanceAmount(balanceState = balanceState)

        // Chart, period selector, and delta only render when there's something
        // to chart. A flat-zero balance hides them entirely — the design says
        // "show 0.00 cleanly, don't draw an empty chart".
        if (hasBalance) {
            Spacer(Modifier.height(10.dp))
            BalanceDelta(chartState = chartState)
            Spacer(Modifier.height(14.dp))
            ChartArea(state = chartState)
            Spacer(Modifier.height(14.dp))
            PeriodSelector(state = chartState)
        }
    }
}

@Composable
private fun BalanceAmount(balanceState: BalanceWidgetState) {
    val c = ZappTheme.colors
    val fiat = balanceState.toFiatFormatted()
    val context = LocalContext.current
    val zec = remember(balanceState.totalBalance, context) {
        stringRes(balanceState.totalBalance, TickerLocation.HIDDEN).getString(context)
    }

    // Swiss display style — Black weight, oversized, tight tracking — matches
    // the wallet hero in the design canvas (52sp whole / 26sp fraction).
    val wholeStyle = ZappTheme.typography.display.copy(
        color = c.text,
        fontSize = 52.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-3).sp,
    )
    val fractionStyle = ZappTheme.typography.displaySecondary.copy(
        color = c.textMuted,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp,
    )

    if (fiat != null) {
        Row {
            BasicText(text = fiat.whole, style = wholeStyle, modifier = Modifier.alignByBaseline())
            BasicText(text = fiat.fraction, style = fractionStyle, modifier = Modifier.alignByBaseline())
        }
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = "$zec ZEC",
            style = ZappTheme.typography.caption.copy(color = c.textMuted),
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(text = zec, style = wholeStyle, modifier = Modifier.alignByBaseline())
            BasicText(text = "ZEC", style = fractionStyle, modifier = Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun BalanceDelta(chartState: BalanceChartState) {
    val c = ZappTheme.colors
    val chartPoints = if (chartState is BalanceChartState.Data) chartState.chart.points else null
    val delta = remember(chartPoints) { chartState.computeDelta() }
    val periodLabel = chartState.periodOrDefault().label()

    if (delta == null) {
        BasicText(
            text = periodLabel,
            style = ZappTheme.typography.caption.copy(color = c.textMuted),
        )
        return
    }

    val sign = if (delta.isPositive) "▲" else "▼"
    val color = if (delta.isPositive) c.success else c.danger

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = "$sign ${delta.valueText}",
            style = ZappTheme.typography.caption.copy(color = color),
        )
        Dot(color = c.textSubtle)
        BasicText(
            text = delta.percentText,
            style = ZappTheme.typography.caption.copy(color = color),
        )
        Dot(color = c.textSubtle)
        BasicText(
            text = periodLabel,
            style = ZappTheme.typography.caption.copy(color = c.textMuted),
        )
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.size(3.dp).background(color, RectangleShape))
}

@Composable
private fun ChartArea(state: BalanceChartState) {
    val c = ZappTheme.colors
    when (state) {
        is BalanceChartState.Data ->
            SparkChart(
                data = state.chart,
                lineColor = c.accent,
                fillColor = c.accent,
                modifier = Modifier.fillMaxWidth(),
            )

        is BalanceChartState.Empty ->
            EmptyChartBox("No data for this period")

        BalanceChartState.Loading ->
            EmptyChartBox("Loading chart…")

        BalanceChartState.Hidden ->
            EmptyChartBox("Balance chart appears here")
    }
}

@Composable
private fun EmptyChartBox(text: String) {
    val c = ZappTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(c.surfaceInput, RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = ZappTheme.typography.caption.copy(color = c.textSubtle),
        )
    }
}

@Composable
private fun PeriodSelector(state: BalanceChartState) {
    val selectedPeriod = state.periodOrDefault()
    val onClick = state.onPeriodClickOrNoop()
    val periods = BalanceChartPeriod.entries
    val labels = periods.map { it.label() }
    val index = periods.indexOf(selectedPeriod).coerceAtLeast(0)

    ZappSegmentedSelector(
        options = labels,
        selectedIndex = index,
        onSelect = { i -> onClick(periods[i]) },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.activitySection(state: ActivityWidgetState) {
    when (state) {
        is ActivityWidgetState.Data -> {
            val lastKey = state.transactions.lastOrNull()?.key
            items(
                items = state.transactions,
                key = { it.key },
            ) { activity ->
                ActivityRow(activity)
                if (activity.key != lastKey) {
                    ZappRowDivider(inset = true)
                }
            }
        }

        is ActivityWidgetState.Empty ->
            item { ActivityEmpty() }

        ActivityWidgetState.Loading ->
            item { ActivityLoading() }
    }
}

@Composable
private fun ActivityRow(state: ActivityState) {
    val c = ZappTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(c.surfaceAlt, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(state.bigIcon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = state.title.getValue(),
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state.subtitle?.let { subtitle ->
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = subtitle.getValue(),
                    style = ZappTheme.typography.rowSubtitle.copy(color = c.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        state.value?.let { styled ->
            val value: AnnotatedString = styled.getValue()
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = value,
                style = ZappTheme.typography.rowTitle.copy(color = c.text),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ActivityEmpty() {
    val c = ZappTheme.colors
    // Swiss-style: left-aligned, no centered illustration, sharp top rule that
    // matches the divider rhythm an actual transaction list would have.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.dp, c.border), RectangleShape)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(c.accent, RectangleShape),
        )
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "No transactions yet.",
            style = ZappTheme.typography.rowTitle.copy(
                color = c.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp,
            ),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = "Your sends and receives will appear here.",
            style = ZappTheme.typography.rowSubtitle.copy(
                color = c.textMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            ),
        )
    }
}

@Composable
private fun ActivityLoading() {
    val c = ZappTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = c.accent)
    }
}

private data class FormattedFiat(val whole: String, val fraction: String)

@Composable
private fun BalanceWidgetState.toFiatFormatted(): FormattedFiat? {
    val exchange = exchangeRate
    if (exchange !is ExchangeRateState.Data) return null
    val conversion = exchange.currencyConversion ?: return null

    return remember(totalBalance, conversion.priceOfZec, exchange.fiatCurrency.symbol) {
        val zec = totalBalance.convertZatoshiToZec()
        val fiatAmount =
            zec.multiply(BigDecimal(conversion.priceOfZec), MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_UP)
        val symbol = exchange.fiatCurrency.symbol
        val whole = fiatAmount.toBigInteger()
        val fractionCents = fiatAmount.subtract(BigDecimal(whole)).multiply(BigDecimal(100)).toInt()
        val wholeFormatted = DecimalFormat("#,###").format(whole)
        FormattedFiat(
            whole = "$symbol$wholeFormatted",
            fraction = ".%02d".format(fractionCents.absoluteValue),
        )
    }
}

private fun Zatoshi.convertZatoshiToZec(): BigDecimal =
    BigDecimal(value).divide(BigDecimal(100_000_000L), 8, RoundingMode.HALF_UP)

private data class BalanceDeltaResult(
    val valueText: String,
    val percentText: String,
    val isPositive: Boolean,
)

private fun BalanceChartState.computeDelta(): BalanceDeltaResult? {
    if (this !is BalanceChartState.Data) return null
    val points: List<SparkChartData.Point> = chart.points
    if (points.size < 2) return null
    val first = points.first().y
    val last = points.last().y
    if (first == 0.0) return null
    val deltaZatoshi = last - first
    val percent = (deltaZatoshi / first) * 100.0
    val deltaZec = BigDecimal(deltaZatoshi).divide(BigDecimal(100_000_000L), 6, RoundingMode.HALF_UP)
    val valueText = "${deltaZec.abs().toPlainString()} ZEC"
    val sign = if (percent >= 0) "+" else "-"
    val percentText = "$sign%.2f%%".format(percent.absoluteValue)
    return BalanceDeltaResult(
        valueText = valueText,
        percentText = percentText,
        isPositive = percent >= 0,
    )
}

private fun BalanceChartState.periodOrDefault(): BalanceChartPeriod =
    when (this) {
        is BalanceChartState.Data -> selectedPeriod
        is BalanceChartState.Empty -> selectedPeriod
        BalanceChartState.Loading, BalanceChartState.Hidden -> BalanceChartPeriod.DEFAULT
    }

private fun BalanceChartState.onPeriodClickOrNoop(): (BalanceChartPeriod) -> Unit =
    when (this) {
        is BalanceChartState.Data -> onPeriodClick
        is BalanceChartState.Empty -> onPeriodClick
        BalanceChartState.Loading, BalanceChartState.Hidden -> { _ -> }
    }

private fun BalanceChartPeriod.label(): String =
    when (this) {
        BalanceChartPeriod.H24 -> "1D"
        BalanceChartPeriod.W1 -> "1W"
        BalanceChartPeriod.M1 -> "1M"
        BalanceChartPeriod.ALL -> "ALL"
    }

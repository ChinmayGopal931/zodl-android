package co.electriccoin.zcash.ui.screen.tabs.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cash.z.ecc.android.sdk.model.Zatoshi
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.wallet.ExchangeRateState
import co.electriccoin.zcash.ui.design.component.chart.SparkChart
import co.electriccoin.zcash.ui.design.component.chart.SparkChartData
import co.electriccoin.zcash.ui.design.component.zapp.ZappSectionLabel
import co.electriccoin.zcash.ui.design.component.zapp.ZappSegmentedSelector
import co.electriccoin.zcash.ui.design.theme.ZappTheme
import co.electriccoin.zcash.ui.design.util.TickerLocation
import co.electriccoin.zcash.ui.design.util.getString
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.balances.BalanceWidgetState
import co.electriccoin.zcash.ui.screen.home.balancechart.BalanceChartPeriod
import co.electriccoin.zcash.ui.screen.home.balancechart.BalanceChartState
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.absoluteValue

@Composable
internal fun BalanceCard(
    balanceState: BalanceWidgetState,
    chartState: BalanceChartState,
    modifier: Modifier = Modifier,
) {
    val c = ZappTheme.colors
    val hasBalance = balanceState.totalBalance.value > 0L

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        ZappSectionLabel(text = stringResource(R.string.home_balance_total_label))
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
    val zec =
        remember(balanceState.totalBalance, context) {
            stringRes(balanceState.totalBalance, TickerLocation.HIDDEN).getString(context)
        }

    // Swiss display style — Black weight, oversized, tight tracking — matches
    // the wallet hero in the design canvas (52sp whole / 26sp fraction).
    val wholeStyle =
        ZappTheme.typography.display.copy(
            color = c.text,
            fontSize = 52.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-3).sp,
        )
    val fractionStyle =
        ZappTheme.typography.displaySecondary.copy(
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
        Row {
            BasicText(
                text = zec,
                style = wholeStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
            BasicText(
                text = " ZEC",
                style = fractionStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
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
        is BalanceChartState.Data -> {
            SparkChart(
                data = state.chart,
                lineColor = c.accent,
                fillColor = c.accent,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is BalanceChartState.Empty -> {
            EmptyChartBox(stringResource(R.string.home_balance_chart_no_data))
        }

        BalanceChartState.Loading -> {
            EmptyChartBox(stringResource(R.string.home_balance_chart_loading))
        }

        BalanceChartState.Hidden -> {
            EmptyChartBox(stringResource(R.string.home_balance_chart_hidden))
        }
    }
}

@Composable
private fun EmptyChartBox(text: String) {
    val c = ZappTheme.colors
    Box(
        modifier =
            Modifier
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

private data class FormattedFiat(
    val whole: String,
    val fraction: String
)

@Composable
private fun BalanceWidgetState.toFiatFormatted(): FormattedFiat? {
    val exchange = exchangeRate
    if (exchange !is ExchangeRateState.Data) return null
    val conversion = exchange.currencyConversion ?: return null

    return remember(totalBalance, conversion.priceOfZec, exchange.fiatCurrency.symbol) {
        val zec = totalBalance.convertZatoshiToZec()
        val fiatAmount =
            zec
                .multiply(BigDecimal(conversion.priceOfZec), MathContext.DECIMAL128)
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

@Composable
private fun BalanceChartPeriod.label(): String = stringResource(labelRes)

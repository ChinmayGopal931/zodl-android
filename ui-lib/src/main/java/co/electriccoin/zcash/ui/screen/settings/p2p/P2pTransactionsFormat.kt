package co.electriccoin.zcash.ui.screen.settings.p2p

import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.util.stringRes
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.OrderStatus
import xyz.justzappit.offramp.p2p.OrderType
import xyz.justzappit.offramp.p2p.P2pOrderHistoryItem
import xyz.justzappit.offramp.p2p.extractUpiVpa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object P2pTransactionsFormat {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val ZERO_BYTE = 0.toByte()

    private val DATE_FORMAT: SimpleDateFormat =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).apply { timeZone = TimeZone.getDefault() }

    fun decodeCurrency(currencyHex: String): String =
        runCatching {
            currencyHex.hexToBytes().takeWhile { it != ZERO_BYTE }.toByteArray().decodeToString().ifBlank { "INR" }
        }.getOrDefault("INR")

    fun timestamp(epochSeconds: Long): String =
        DATE_FORMAT.format(Date(epochSeconds * MILLIS_PER_SECOND))
}

internal fun P2pOrderHistoryItem.toRow(network: P2pNetworkConfig): P2pTransactionRow {
    val currency = P2pTransactionsFormat.decodeCurrency(currencyHex)
    val tone = when (status) {
        OrderStatus.COMPLETED -> P2pTransactionRow.StatusTone.Success
        OrderStatus.CANCELLED -> P2pTransactionRow.StatusTone.Cancelled
        OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PAID -> P2pTransactionRow.StatusTone.Pending
    }
    return P2pTransactionRow(
        orderId = orderId.toString(),
        typeLabel = stringRes(typeLabelRes(orderType)),
        statusLabel = stringRes(statusLabelRes(status)),
        statusTone = tone,
        amountUsdc = stringRes(R.string.p2p_transactions_row_amount_usdc, usdcAmount.toDisplayString(stripTrailingZeros = true)),
        amountFiat = stringRes(
            R.string.p2p_transactions_row_amount_fiat,
            fiatAmount.toDisplayString(stripTrailingZeros = true),
            currency,
        ),
        fromLabel = fromForType(orderType)?.let(::extractUpiVpa)?.let { stringRes(R.string.p2p_transactions_row_from, it) },
        toLabel = toForType(orderType)?.let(::extractUpiVpa)?.let { stringRes(R.string.p2p_transactions_row_to, it) },
        timestamp = (completedAtEpochSeconds ?: cancelledAtEpochSeconds ?: placedAtEpochSeconds)
            ?.let { stringRes(P2pTransactionsFormat.timestamp(it)) },
        explorerUrl = null,
    )
}

private fun P2pOrderHistoryItem.fromForType(type: OrderType): String? = when (type) {
    // BUY: user receives fiat; "from" = merchant's pay-to VPA (encrypted in encUpi at acceptOrder).
    OrderType.BUY -> recipientUpiPlain
    // PAY/SELL: user pays fiat out; "from" = merchant's UPI (encrypted in encMerchantUpi on completion).
    OrderType.PAY, OrderType.SELL -> merchantUpiPlain
}

private fun P2pOrderHistoryItem.toForType(type: OrderType): String? = when (type) {
    OrderType.BUY -> null
    OrderType.PAY, OrderType.SELL -> recipientUpiPlain
}

private fun typeLabelRes(orderType: OrderType): Int = when (orderType) {
    OrderType.BUY -> R.string.p2p_transactions_type_buy
    OrderType.SELL -> R.string.p2p_transactions_type_sell
    OrderType.PAY -> R.string.p2p_transactions_type_pay
}

private fun statusLabelRes(status: OrderStatus): Int = when (status) {
    OrderStatus.PLACED -> R.string.p2p_transactions_status_placed
    OrderStatus.ACCEPTED -> R.string.p2p_transactions_status_accepted
    OrderStatus.PAID -> R.string.p2p_transactions_status_paid
    OrderStatus.COMPLETED -> R.string.p2p_transactions_status_completed
    OrderStatus.CANCELLED -> R.string.p2p_transactions_status_cancelled
}

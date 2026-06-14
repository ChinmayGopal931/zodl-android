package co.electriccoin.zcash.ui.screen.settings.p2p

import co.electriccoin.zcash.ui.design.util.StringResource

data class P2pTransactionsState(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val isRefreshing: Boolean,
    val balance: BalanceState,
    val refund: RefundUiState,
    val confirmRefund: ConfirmRefundDialog?,
    val rows: List<P2pTransactionRow>,
    val emptyMessage: StringResource?,
    val errorMessage: StringResource?,
)

sealed interface BalanceState {
    data object Loading : BalanceState

    data class Loaded(
        val balanceUsdc: StringResource,
        val accountAddressShort: String,
        val accountExplorerUrl: String?,
    ) : BalanceState

    data object Unavailable : BalanceState
}

/**
 * Drives the inline Refund control next to the balance. `Hidden` covers every case the user
 * shouldn't see the button: testnet (no NEAR route), zero balance, balance still loading.
 */
sealed interface RefundUiState {
    data object Hidden : RefundUiState

    data class Available(
        val onClick: () -> Unit
    ) : RefundUiState

    data object InProgress : RefundUiState

    data class FailedRetry(
        val message: StringResource,
        val onRetry: () -> Unit
    ) : RefundUiState
}

data class ConfirmRefundDialog(
    val amount: StringResource,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

data class P2pTransactionRow(
    val orderId: String,
    val typeLabel: StringResource,
    val statusLabel: StringResource,
    val statusTone: StatusTone,
    val amountUsdc: StringResource,
    val amountFiat: StringResource,
    val fromLabel: StringResource?,
    val toLabel: StringResource?,
    val timestamp: StringResource?,
    val explorerUrl: String?,
    val detail: TransactionDetail?,
) {
    enum class StatusTone { Pending, Success, Cancelled, Failed }
}

/**
 * Extra fields revealed when the user expands a transaction row. Sourced from the same
 * [xyz.justzappit.offramp.p2p.P2pOrderHistoryItem]; the merchant UPI is already decrypted via
 * the relay private key in [xyz.justzappit.offramp.p2p.P2pOrderHistorySource].
 */
data class TransactionDetail(
    val merchantUpiPlain: String?,
    val merchantAddressShort: String?,
    val merchantExplorerUrl: String?,
    val placedAt: StringResource?,
    val duration: StringResource?,
)

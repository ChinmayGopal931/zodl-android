package co.electriccoin.zcash.ui.screen.swap.upi.progress

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResource

internal enum class UpiOfframpStepStatus { Pending, InProgress, Completed, Failed }

internal data class UpiOfframpStep(
    val label: StringResource,
    val status: UpiOfframpStepStatus,
    val txHash: String? = null,
    val txExplorerUrl: String? = null,
    val detailLines: List<StringResource> = emptyList(),
)

internal data class UpiOfframpOrderSummary(
    val amountUsdcDisplay: StringResource,
    val recipient: String,
    val orderId: String?,
    val networkName: String,
    // Null until the counterfactual smart-account address resolves (one async factory call).
    val signerAddress: String?,
    val signerExplorerUrl: String?,
    /** "Completed in 1m 32s" — populated only on a completed order. */
    val completionDuration: StringResource? = null,
    /** "DD MMM YYYY, hh:mm a" formatted timestamp; populated on Completed and Cancelled. */
    val terminalTimestamp: StringResource? = null,
    /** Merchant address surfaced as a labelled, ellipsised, copyable line on terminal screens. */
    val merchantAddress: String? = null,
    val merchantExplorerUrl: String? = null,
)

/**
 * Three-line "You send / Fee / You receive" card sourced from `getAdditionalOrderDetails`.
 * Fields populate incrementally as the order moves on-chain; we only render the card once at
 * least one value is non-null so we don't show all-zeros pre-acceptance.
 */
internal data class UpiOfframpFeeBreakdown(
    val youSend: StringResource?,
    val fee: StringResource?,
    val youReceive: StringResource?,
)

internal data class UpiOfframpFailureCard(
    val stepLabel: StringResource,
    val decodedReason: StringResource?,
    val rawSelector: String?,
    val rawMessage: String,
    val txHash: String?,
    val txExplorerUrl: String?,
)

/**
 * Terminal "transaction cancelled" card. Distinct from [UpiOfframpFailureCard] — cancellation
 * is a normal on-chain outcome (contract auto-cancelled, USDC refunded) and uses neutral styling
 * rather than the red-bordered failure box.
 */
internal data class UpiOfframpCancelledCard(
    val refundedAmount: StringResource?,
    val cancelledAt: StringResource?,
    val tip: StringResource,
)

/** Shown after a successful pullback (bridgeFundsBackToZec → FundsRecovered). */
internal data class UpiOfframpRecoveryCard(
    val amount: StringResource,
    val target: String?,
    val txHash: String?,
    val txExplorerUrl: String?,
)

internal data class UpiOfframpProgressState(
    val title: StringResource,
    val subtitle: StringResource?,
    val summary: UpiOfframpOrderSummary?,
    val feeBreakdown: UpiOfframpFeeBreakdown?,
    val steps: List<UpiOfframpStep>,
    val failure: UpiOfframpFailureCard?,
    val cancelled: UpiOfframpCancelledCard?,
    val recovery: UpiOfframpRecoveryCard? = null,
    val primaryButton: ButtonState?,
    val onBack: () -> Unit,
)

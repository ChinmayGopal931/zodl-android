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
    val signerAddress: String,
    val signerExplorerUrl: String,
)

internal data class UpiOfframpFailureCard(
    val stepLabel: StringResource,
    val decodedReason: StringResource?,
    val rawSelector: String?,
    val rawMessage: String,
    val txHash: String?,
    val txExplorerUrl: String?,
)

internal data class UpiOfframpProgressState(
    val title: StringResource,
    val subtitle: StringResource?,
    val summary: UpiOfframpOrderSummary?,
    val steps: List<UpiOfframpStep>,
    val failure: UpiOfframpFailureCard?,
    val primaryButton: ButtonState?,
    val onBack: () -> Unit,
)

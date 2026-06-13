package co.electriccoin.zcash.ui.screen.swap.upi

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource

internal data class UpiOfframpState(
    val onScanQr: () -> Unit,
    val upiField: TextFieldState,
    val inrInput: NumberTextFieldState,
    val usdcEquivalent: StringResource?,
    val rateText: StringResource,
    val infoText: StringResource?,
    val errorText: StringResource?,
    val sendButton: ButtonState,
    val onHistoryClick: () -> Unit,
    val onAddFunds: () -> Unit,
    val baseBalanceText: StringResource? = null,
    val fundingPlanText: StringResource? = null,
    /** Forgets an in-flight checkpoint locally, without touching the on-chain order. */
    val onDiscardInFlight: (() -> Unit)? = null,
)

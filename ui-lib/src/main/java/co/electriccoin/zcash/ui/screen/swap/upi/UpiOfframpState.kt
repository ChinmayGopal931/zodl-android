package co.electriccoin.zcash.ui.screen.swap.upi

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource

internal enum class UpiOfframpAmountSide {
    USDC,
    INR,
}

internal data class UpiOfframpState(
    val primary: UpiOfframpAmountSide,
    val usdcInput: NumberTextFieldState,
    val inrInput: NumberTextFieldState,
    val onSwapSides: () -> Unit,
    val rateText: StringResource,
    val upiField: TextFieldState,
    val infoText: StringResource?,
    val errorText: StringResource?,
    val sendButton: ButtonState,
    val onScanQr: () -> Unit,
    /**
     * Smart account's current USDC balance on Base, shown inline above the USDC input. Null until the
     * first balance read returns (treated as unknown — UI hides the label).
     */
    val baseBalanceText: StringResource? = null,
    /**
     * One-line summary of how the entered order amount will be funded — either reused from the Base
     * balance (no bridge) or bridged from ZEC. Null when there's nothing meaningful to say (no amount
     * entered, balance still loading).
     */
    val fundingPlanText: StringResource? = null,
    /** Local-only escape hatch: forget an in-flight checkpoint without touching the on-chain order. */
    val onDiscardInFlight: (() -> Unit)? = null,
)

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
)

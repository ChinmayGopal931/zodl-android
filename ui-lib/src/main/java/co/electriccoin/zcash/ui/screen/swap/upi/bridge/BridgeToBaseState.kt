package co.electriccoin.zcash.ui.screen.swap.upi.bridge

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpStep

internal data class BridgeToBaseState(
    val amountInput: NumberTextFieldState,
    val baseBalanceText: StringResource?,
    /** "≈ ₹X at the current rate" for the entered USDC. */
    val inrValueText: StringResource?,
    /** "Estimated time: ~N min" (or a static fallback). */
    val etaText: StringResource?,
    val availabilityHint: BridgeAvailabilityHint?,
    val explainer: StringResource,
    val errorText: StringResource?,
    /** Empty while the user is still entering an amount; the small bridge step list once it starts. */
    val steps: List<UpiOfframpStep>,
    val isInputVisible: Boolean,
    val primaryButton: ButtonState,
    val onBack: () -> Unit,
)

/** One-line "merchants available / none right now" hint shown before bridging. [isWarning] tints it amber. */
internal data class BridgeAvailabilityHint(
    val text: StringResource,
    val isWarning: Boolean,
)

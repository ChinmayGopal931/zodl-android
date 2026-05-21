package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.InnerTextFieldState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.component.TextSelection
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode

internal class UpiOfframpVM(
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val primary = MutableStateFlow(UpiOfframpAmountSide.INR)
    private val usdcState = MutableStateFlow(NumberTextFieldInnerState())
    private val inrState = MutableStateFlow(NumberTextFieldInnerState())
    private val upiText = MutableStateFlow("")

    val state: StateFlow<UpiOfframpState> =
        combine(primary, usdcState, inrState, upiText) { side, usdc, inr, upi ->
            buildState(side, usdc, inr, upi)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = buildState(
                side = primary.value,
                usdc = usdcState.value,
                inr = inrState.value,
                upi = upiText.value,
            ),
        )

    private fun buildState(
        side: UpiOfframpAmountSide,
        usdc: NumberTextFieldInnerState,
        inr: NumberTextFieldInnerState,
        upi: String,
    ): UpiOfframpState {
        val usdcAmount = usdc.amount
        val validationError = validate(usdcAmount, upi)
        return UpiOfframpState(
            primary = side,
            usdcInput = NumberTextFieldState(innerState = usdc, onValueChange = ::onUsdcChange),
            inrInput = NumberTextFieldState(innerState = inr, onValueChange = ::onInrChange),
            onSwapSides = ::onSwapSides,
            rateText = stringRes(R.string.upi_offramp_rate_label, RATE_DISPLAY_STRING),
            upiField = TextFieldState(stringRes(upi)) { newValue -> onUpiChange(newValue) },
            scanButton = IconButtonState(
                icon = R.drawable.qr_code_icon,
                onClick = ::onScanClick,
            ),
            infoText = if (validationError == null && usdcAmount != null && usdcAmount > BigDecimal.ZERO) {
                stringRes(R.string.upi_offramp_estimate_disclaimer)
            } else null,
            errorText = validationError,
            sendButton = ButtonState(
                text = stringRes(R.string.upi_offramp_send_button),
                isEnabled = validationError == null &&
                    usdcAmount != null &&
                    usdcAmount > BigDecimal.ZERO &&
                    upi.isNotBlank() &&
                    UPI_HANDLE_REGEX.matches(upi),
                onClick = ::onSendClick,
            ),
        )
    }

    private fun validate(usdc: BigDecimal?, upi: String): StringResource? {
        if (usdc != null && usdc > USDC_CAP) return stringRes(R.string.upi_offramp_error_above_cap)
        if (upi.isNotBlank() && !UPI_HANDLE_REGEX.matches(upi)) {
            return stringRes(R.string.upi_offramp_error_invalid_upi)
        }
        return null
    }

    private fun onUsdcChange(next: NumberTextFieldInnerState) {
        primary.value = UpiOfframpAmountSide.USDC
        usdcState.value = next
        val derivedInr = next.amount?.multiply(RATE_INR_PER_USDC)?.setScale(INR_DECIMALS, RoundingMode.HALF_UP)
        inrState.value = if (derivedInr == null) {
            NumberTextFieldInnerState()
        } else {
            NumberTextFieldInnerState.fromAmount(derivedInr)
        }
    }

    private fun onInrChange(next: NumberTextFieldInnerState) {
        primary.value = UpiOfframpAmountSide.INR
        inrState.value = next
        val derivedUsdc = next.amount?.divide(RATE_INR_PER_USDC, USDC_DECIMALS, RoundingMode.HALF_UP)
        usdcState.value = if (derivedUsdc == null) {
            NumberTextFieldInnerState()
        } else {
            NumberTextFieldInnerState.fromAmount(derivedUsdc)
        }
    }

    private fun onUpiChange(next: String) {
        upiText.value = next.trim()
    }

    private fun onSwapSides() {
        primary.value = when (primary.value) {
            UpiOfframpAmountSide.INR -> UpiOfframpAmountSide.USDC
            UpiOfframpAmountSide.USDC -> UpiOfframpAmountSide.INR
        }
    }

    private fun onScanClick() {
        // TODO Phase 5.1: navigate to ScanGenericAddressArgs and parse the upi:// QR result.
    }

    private fun onSendClick() {
        val usdcAmount = usdcState.value.amount ?: return
        if (usdcAmount <= BigDecimal.ZERO || usdcAmount > USDC_CAP) return
        val upi = upiText.value
        if (upi.isBlank() || !UPI_HANDLE_REGEX.matches(upi)) return
        val usdcMicro = usdcAmount.movePointRight(USDC_CONTRACT_DECIMALS).toBigInteger()
        navigationRouter.forward(
            UpiOfframpProgressArgs(
                recipientUpi = upi,
                usdcAmountMicro = usdcMicro.toString(),
                currency = "INR",
            ),
        )
    }

    companion object {
        // Hardcoded rate for v1 — TODO read getPriceConfig(currency) on-chain or a Coingecko fallback.
        private val RATE_INR_PER_USDC: BigDecimal = BigDecimal("85.0")
        private const val RATE_DISPLAY_STRING: String = "85"

        // v1 spec: PAY orders below $99 USDC bypass the on-chain RP gate.
        private val USDC_CAP: BigDecimal = BigDecimal("99")

        private const val USDC_DECIMALS = 4
        private const val USDC_CONTRACT_DECIMALS = 6
        private const val INR_DECIMALS = 2

        private val UPI_HANDLE_REGEX = Regex("^[A-Za-z0-9._\\-]{1,256}@[A-Za-z]{2,64}\$")
    }
}

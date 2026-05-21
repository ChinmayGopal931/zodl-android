package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.usecase.GetUpiOfframpRateUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import xyz.justzappit.offramp.p2p.CurrencyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

internal class UpiOfframpVM(
    private val navigationRouter: NavigationRouter,
    private val getRate: GetUpiOfframpRateUseCase,
) : ViewModel() {
    private val primary = MutableStateFlow(UpiOfframpAmountSide.INR)
    private val usdcState = MutableStateFlow(NumberTextFieldInnerState())
    private val inrState = MutableStateFlow(NumberTextFieldInnerState())
    private val upiText = MutableStateFlow("")
    private val rate = MutableStateFlow(FALLBACK_RATE)

    val state: StateFlow<UpiOfframpState> =
        combine(primary, usdcState, inrState, upiText, rate) { side, usdc, inr, upi, currentRate ->
            buildState(side, usdc, inr, upi, currentRate)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = buildState(
                side = primary.value,
                usdc = usdcState.value,
                inr = inrState.value,
                upi = upiText.value,
                currentRate = rate.value,
            ),
        )

    init {
        viewModelScope.launch { refreshRate() }
    }

    private suspend fun refreshRate() {
        val newRate = getRate(CURRENCY) ?: return
        Twig.info { "UpiOfframpVM: live sellPrice for ${CURRENCY.code} = $newRate" }
        rate.update { newRate }
        rederiveAfterRateChange(newRate)
    }

    private fun rederiveAfterRateChange(newRate: BigDecimal) {
        when (primary.value) {
            UpiOfframpAmountSide.USDC -> usdcState.value.amount?.let { usdc ->
                inrState.update {
                    NumberTextFieldInnerState.fromAmount(
                        usdc.multiply(newRate).setScale(INR_DECIMALS, RoundingMode.HALF_UP),
                    )
                }
            }
            UpiOfframpAmountSide.INR -> inrState.value.amount?.let { inr ->
                usdcState.update {
                    NumberTextFieldInnerState.fromAmount(
                        inr.divide(newRate, USDC_DECIMALS, RoundingMode.HALF_UP),
                    )
                }
            }
        }
    }

    private fun buildState(
        side: UpiOfframpAmountSide,
        usdc: NumberTextFieldInnerState,
        inr: NumberTextFieldInnerState,
        upi: String,
        currentRate: BigDecimal,
    ): UpiOfframpState {
        val usdcAmount = usdc.amount
        val validationError = validate(usdcAmount, upi)
        val rateDisplay = currentRate.stripTrailingZeros().toPlainString()
        return UpiOfframpState(
            primary = side,
            usdcInput = NumberTextFieldState(innerState = usdc, onValueChange = ::onUsdcChange),
            inrInput = NumberTextFieldState(innerState = inr, onValueChange = ::onInrChange),
            onSwapSides = ::onSwapSides,
            rateText = stringRes(R.string.upi_offramp_rate_label, rateDisplay),
            upiField = TextFieldState(stringRes(upi)) { newValue -> onUpiChange(newValue) },
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
        primary.update { UpiOfframpAmountSide.USDC }
        usdcState.update { next }
        val currentRate = rate.value
        val derivedInr = next.amount?.multiply(currentRate)?.setScale(INR_DECIMALS, RoundingMode.HALF_UP)
        inrState.update {
            if (derivedInr == null) NumberTextFieldInnerState() else NumberTextFieldInnerState.fromAmount(derivedInr)
        }
    }

    private fun onInrChange(next: NumberTextFieldInnerState) {
        primary.update { UpiOfframpAmountSide.INR }
        inrState.update { next }
        val currentRate = rate.value
        val derivedUsdc = next.amount?.divide(currentRate, USDC_DECIMALS, RoundingMode.HALF_UP)
        usdcState.update {
            if (derivedUsdc == null) NumberTextFieldInnerState() else NumberTextFieldInnerState.fromAmount(derivedUsdc)
        }
    }

    private fun onUpiChange(next: String) {
        upiText.update { next.trim() }
    }

    private fun onSwapSides() {
        primary.update {
            when (it) {
                UpiOfframpAmountSide.INR -> UpiOfframpAmountSide.USDC
                UpiOfframpAmountSide.USDC -> UpiOfframpAmountSide.INR
            }
        }
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
                currency = CURRENCY,
            ),
        )
    }

    companion object {
        private val CURRENCY = CurrencyCode.Inr

        // Matches the FE: priceConfig?.sellPrice ?? 85.
        private val FALLBACK_RATE: BigDecimal = BigDecimal("85")

        // v1 spec: PAY orders below $99 USDC bypass the on-chain RP gate.
        private val USDC_CAP: BigDecimal = BigDecimal("99")

        private const val USDC_DECIMALS = 4
        private const val USDC_CONTRACT_DECIMALS = 6
        private const val INR_DECIMALS = 2

        private val UPI_HANDLE_REGEX = Regex("^[A-Za-z0-9._\\-]{1,256}@[A-Za-z]{2,64}\$")
    }
}

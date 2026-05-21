package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.PriceConfigDecoder
import java.math.BigDecimal
import java.math.RoundingMode

internal class UpiOfframpVM(
    private val navigationRouter: NavigationRouter,
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
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
        viewModelScope.launch { fetchPriceConfig() }
    }

    private suspend fun fetchPriceConfig() {
        runCatching {
            val raw = rpc.ethCall(
                to = network.diamondAddress,
                data = DiamondCalls.getPriceConfigCalldata(CURRENCY),
            )
            PriceConfigDecoder.decode(raw).sellPriceAsRate()
        }.onSuccess { newRate ->
            Twig.info { "UpiOfframpVM: live sellPrice for $CURRENCY = $newRate" }
            rate.value = newRate
            rederiveAfterRateChange(newRate)
        }.onFailure { cause ->
            Twig.warn(cause) { "UpiOfframpVM: getPriceConfig failed, falling back to $FALLBACK_RATE" }
        }
    }

    private fun rederiveAfterRateChange(newRate: BigDecimal) {
        when (primary.value) {
            UpiOfframpAmountSide.USDC -> usdcState.value.amount?.let { usdc ->
                inrState.value = NumberTextFieldInnerState.fromAmount(
                    usdc.multiply(newRate).setScale(INR_DECIMALS, RoundingMode.HALF_UP),
                )
            }
            UpiOfframpAmountSide.INR -> inrState.value.amount?.let { inr ->
                usdcState.value = NumberTextFieldInnerState.fromAmount(
                    inr.divide(newRate, USDC_DECIMALS, RoundingMode.HALF_UP),
                )
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
        val currentRate = rate.value
        val derivedInr = next.amount?.multiply(currentRate)?.setScale(INR_DECIMALS, RoundingMode.HALF_UP)
        inrState.value = if (derivedInr == null) {
            NumberTextFieldInnerState()
        } else {
            NumberTextFieldInnerState.fromAmount(derivedInr)
        }
    }

    private fun onInrChange(next: NumberTextFieldInnerState) {
        primary.value = UpiOfframpAmountSide.INR
        inrState.value = next
        val currentRate = rate.value
        val derivedUsdc = next.amount?.divide(currentRate, USDC_DECIMALS, RoundingMode.HALF_UP)
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
                currency = CURRENCY,
            ),
        )
    }

    companion object {
        private const val CURRENCY = "INR"

        // Matches the official FE behaviour: if the on-chain price read fails, fall back to 85.
        // See user-app-client/src/pages/pay/index.tsx:259 (priceConfig?.sellPrice ?? 85).
        private val FALLBACK_RATE: BigDecimal = BigDecimal("85")

        // v1 spec: PAY orders below $99 USDC bypass the on-chain RP gate.
        private val USDC_CAP: BigDecimal = BigDecimal("99")

        private const val USDC_DECIMALS = 4
        private const val USDC_CONTRACT_DECIMALS = 6
        private const val INR_DECIMALS = 2

        private val UPI_HANDLE_REGEX = Regex("^[A-Za-z0-9._\\-]{1,256}@[A-Za-z]{2,64}\$")
    }
}

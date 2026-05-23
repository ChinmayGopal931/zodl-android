package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.OfframpRepository
import co.electriccoin.zcash.ui.common.usecase.GetUpiOfframpRateUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanUpiUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import xyz.justzappit.offramp.orchestrator.OfframpCheckpoint
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.UpiQrParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

internal class UpiOfframpVM(
    private val navigationRouter: NavigationRouter,
    private val getRate: GetUpiOfframpRateUseCase,
    private val offrampRepo: OfframpRepository,
    private val navigateToScanUpi: NavigateToScanUpiUseCase,
) : ViewModel() {
    private val primary = MutableStateFlow(UpiOfframpAmountSide.INR)
    private val usdcState = MutableStateFlow(NumberTextFieldInnerState())
    private val inrState = MutableStateFlow(NumberTextFieldInnerState())
    private val upiText = MutableStateFlow("")
    private val rate = MutableStateFlow(FALLBACK_RATE)
    private val inFlight = MutableStateFlow<OfframpCheckpoint?>(null)

    val state: StateFlow<UpiOfframpState> =
        combine(
            combine(primary, usdcState, inrState) { side, usdc, inr -> Triple(side, usdc, inr) },
            upiText,
            rate,
            inFlight,
        ) { amounts, upi, currentRate, checkpoint ->
            buildState(
                side = amounts.first,
                usdc = amounts.second,
                inr = amounts.third,
                upi = upi,
                currentRate = currentRate,
                inFlightCheckpoint = checkpoint,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = buildState(
                side = primary.value,
                usdc = usdcState.value,
                inr = inrState.value,
                upi = upiText.value,
                currentRate = rate.value,
                inFlightCheckpoint = inFlight.value,
            ),
        )

    init {
        viewModelScope.launch {
            // §5f: refetch every 30s so the quote tracks the rate the contract will stamp.
            while (isActive) {
                refreshRate()
                delay(RATE_REFRESH_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            offrampRepo.observeInFlight().collect { checkpoint -> inFlight.update { checkpoint } }
        }
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
                        usdc.multiply(newRate).setScale(INR_DECIMALS, RoundingMode.FLOOR),
                    )
                }
            }
            UpiOfframpAmountSide.INR -> inrState.value.amount?.let { inr ->
                usdcState.update {
                    NumberTextFieldInnerState.fromAmount(
                        inr.divide(newRate, USDC_DECIMALS, RoundingMode.FLOOR),
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
        inFlightCheckpoint: OfframpCheckpoint?,
    ): UpiOfframpState {
        val usdcAmount = usdc.amount
        val validationError = if (inFlightCheckpoint != null) {
            stringRes(R.string.upi_offramp_error_in_flight)
        } else {
            validate(usdcAmount, upi)
        }
        val rateDisplay = currentRate.stripTrailingZeros().toPlainString()
        val sendButtonText = if (inFlightCheckpoint != null) {
            stringRes(R.string.upi_offramp_resume_button)
        } else {
            stringRes(R.string.upi_offramp_send_button)
        }
        val sendEnabled = if (inFlightCheckpoint != null) {
            true
        } else {
            validationError == null &&
                usdcAmount != null &&
                usdcAmount > BigDecimal.ZERO &&
                upi.isNotBlank() &&
                UpiQrParser.validateUpiId(upi)
        }
        return UpiOfframpState(
            primary = side,
            usdcInput = NumberTextFieldState(innerState = usdc, onValueChange = ::onUsdcChange),
            inrInput = NumberTextFieldState(innerState = inr, onValueChange = ::onInrChange),
            onSwapSides = ::onSwapSides,
            rateText = stringRes(R.string.upi_offramp_rate_label, rateDisplay),
            upiField = TextFieldState(stringRes(upi)) { newValue -> onUpiChange(newValue) },
            infoText = if (inFlightCheckpoint == null &&
                validationError == null &&
                usdcAmount != null &&
                usdcAmount > BigDecimal.ZERO
            ) {
                stringRes(R.string.upi_offramp_estimate_disclaimer)
            } else null,
            errorText = validationError,
            sendButton = ButtonState(
                text = sendButtonText,
                isEnabled = sendEnabled,
                onClick = ::onSendClick,
            ),
            onScanQr = ::onScanQr,
            onDiscardInFlight = if (inFlightCheckpoint != null) ::onDiscardInFlight else null,
        )
    }

    private fun onDiscardInFlight() {
        viewModelScope.launch { offrampRepo.clear() }
    }

    private fun validate(usdc: BigDecimal?, upi: String): StringResource? {
        if (usdc != null && usdc > USDC_CAP) return stringRes(R.string.upi_offramp_error_above_cap)
        if (upi.isNotBlank() && !UpiQrParser.validateUpiId(upi)) {
            return stringRes(R.string.upi_offramp_error_invalid_upi)
        }
        return null
    }

    private fun onScanQr() {
        viewModelScope.launch {
            val result = navigateToScanUpi() ?: return@launch
            upiText.update { result.paymentAddress }
            result.fiatAmount?.let { fiat ->
                // QR included an `am=` field; pre-fill the INR side as the user's primary so the
                // USDC side derives from the live rate (matches the SDK's `parseUPI` semantics).
                primary.update { UpiOfframpAmountSide.INR }
                inrState.update {
                    NumberTextFieldInnerState.fromAmount(fiat.setScale(INR_DECIMALS, RoundingMode.FLOOR))
                }
                val currentRate = rate.value
                usdcState.update {
                    NumberTextFieldInnerState.fromAmount(
                        fiat.divide(currentRate, USDC_DECIMALS, RoundingMode.FLOOR),
                    )
                }
            }
        }
    }

    private fun onUsdcChange(next: NumberTextFieldInnerState) {
        primary.update { UpiOfframpAmountSide.USDC }
        usdcState.update { next }
        val currentRate = rate.value
        val derivedInr = next.amount?.multiply(currentRate)?.setScale(INR_DECIMALS, RoundingMode.FLOOR)
        inrState.update {
            if (derivedInr == null) NumberTextFieldInnerState() else NumberTextFieldInnerState.fromAmount(derivedInr)
        }
    }

    private fun onInrChange(next: NumberTextFieldInnerState) {
        primary.update { UpiOfframpAmountSide.INR }
        inrState.update { next }
        val currentRate = rate.value
        val derivedUsdc = next.amount?.divide(currentRate, USDC_DECIMALS, RoundingMode.FLOOR)
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
        // If a checkpoint is in flight, jump back into the progress screen — it'll resume.
        val existing = inFlight.value
        if (existing != null) {
            navigationRouter.forward(
                UpiOfframpProgressArgs(
                    recipientUpi = existing.recipientUpi,
                    usdcAmountMicro = existing.usdcAmountMicroDecimal,
                    // Old checkpoints lack fiat — orchestrator resolves a fallback at resume time.
                    fiatAmountMicro = existing.fiatAmountMicroDecimal ?: existing.usdcAmountMicroDecimal,
                    payeeName = existing.payeeName,
                    currency = existing.currency,
                ),
            )
            return
        }
        val usdcAmount = usdcState.value.amount ?: return
        val inrAmount = inrState.value.amount ?: return
        if (usdcAmount <= BigDecimal.ZERO || usdcAmount > USDC_CAP) return
        if (inrAmount <= BigDecimal.ZERO) return
        val upi = upiText.value
        if (upi.isBlank() || !UpiQrParser.validateUpiId(upi)) return
        val usdcMicro = usdcAmount.movePointRight(USDC_CONTRACT_DECIMALS).toBigInteger()
        val fiatMicro = inrAmount.movePointRight(USDC_CONTRACT_DECIMALS).toBigInteger()
        navigationRouter.forward(
            UpiOfframpProgressArgs(
                recipientUpi = upi,
                usdcAmountMicro = usdcMicro.toString(),
                fiatAmountMicro = fiatMicro.toString(),
                currency = CURRENCY,
            ),
        )
    }

    companion object {
        private val CURRENCY = CurrencyCode.Inr

        // Matches the FE: priceConfig?.sellPrice ?? 85.
        private val FALLBACK_RATE: BigDecimal = BigDecimal("85")

        // p2p.me caps a single offramp at 100 USDC. Surfaced proactively in the UI via
        // R.string.upi_offramp_limit_hint and enforced here as a hard input cap.
        private val USDC_CAP: BigDecimal = BigDecimal("100")

        private const val USDC_DECIMALS = 4
        private const val USDC_CONTRACT_DECIMALS = 6
        private const val INR_DECIMALS = 2
        private const val RATE_REFRESH_INTERVAL_MS = 30_000L
    }
}

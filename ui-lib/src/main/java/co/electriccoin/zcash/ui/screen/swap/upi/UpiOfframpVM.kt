package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProvider
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanUpiUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.account.SmartOfframpAccountProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.config.P2pNetworks
import xyz.justzappit.offramp.orchestrator.OfframpCheckpoint
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.UpiQrParser
import xyz.justzappit.offramp.p2p.Usdc6
import xyz.justzappit.offramp.p2p.getPriceConfig
import xyz.justzappit.offramp.p2p.getUsdcBalance
import java.math.BigDecimal
import java.math.RoundingMode

internal class UpiOfframpVM(
    private val navigationRouter: NavigationRouter,
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
    private val accountProvider: SmartOfframpAccountProvider,
    private val checkpointStorage: OfframpCheckpointStorageProvider,
    private val navigateToScanUpi: NavigateToScanUpiUseCase,
) : ViewModel() {
    private val primary = MutableStateFlow(UpiOfframpAmountSide.INR)
    private val usdcState = MutableStateFlow(NumberTextFieldInnerState())
    private val inrState = MutableStateFlow(NumberTextFieldInnerState())
    private val upiText = MutableStateFlow("")
    private val rate = MutableStateFlow(FALLBACK_RATE)
    private val inFlight = MutableStateFlow<OfframpCheckpoint?>(null)
    private val baseBalance = MutableStateFlow<Usdc6?>(null)

    /**
     * Resolved once on init — the smart account address is deterministic from the owner key and
     * doesn't change across rebuilds. Null while the first factory.getAddress call is in flight.
     */
    private var smartAccountAddress: Address? = null

    val state: StateFlow<UpiOfframpState> =
        combine(
            combine(primary, usdcState, inrState) { side, usdc, inr -> Triple(side, usdc, inr) },
            upiText,
            rate,
            inFlight,
            baseBalance,
        ) { amounts, upi, currentRate, checkpoint, balance ->
            buildState(
                side = amounts.first,
                usdc = amounts.second,
                inr = amounts.third,
                upi = upi,
                currentRate = currentRate,
                inFlightCheckpoint = checkpoint,
                balance = balance,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue =
                buildState(
                    side = primary.value,
                    usdc = usdcState.value,
                    inr = inrState.value,
                    upi = upiText.value,
                    currentRate = rate.value,
                    inFlightCheckpoint = inFlight.value,
                    balance = baseBalance.value,
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
            checkpointStorage.observe().collect { checkpoint -> inFlight.update { checkpoint } }
        }
        viewModelScope.launch {
            // Resolve the smart account once, then poll its USDC balance on the same cadence as the
            // rate. Surfacing the balance lets the user see when prior cancelled orders left USDC on
            // Base — those funds reuse without a new NEAR bridge.
            smartAccountAddress =
                runCatching { accountProvider.resolve().address }
                    .onFailure { Twig.warn(it) { "UpiOfframpVM: smart account resolve failed" } }
                    .getOrNull()
            if (smartAccountAddress == null) return@launch
            while (isActive) {
                refreshBaseBalance()
                delay(BALANCE_REFRESH_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshBaseBalance() {
        val account = smartAccountAddress ?: return
        val fetched =
            runCatching { rpc.getUsdcBalance(network.usdcAddress, account) }
                .onFailure { Twig.warn(it) { "UpiOfframpVM: getUsdcBalance failed" } }
                .getOrNull() ?: return
        baseBalance.update { fetched }
    }

    private suspend fun refreshRate() {
        val newRate =
            runCatching { rpc.getPriceConfig(network.diamondAddress, CURRENCY).sellPriceAsRate() }
                .onFailure { Twig.warn(it) { "UpiOfframpVM: getPriceConfig(${CURRENCY.code}) failed" } }
                .getOrNull() ?: return
        Twig.info { "UpiOfframpVM: live sellPrice for ${CURRENCY.code} = $newRate" }
        rate.update { newRate }
        rederiveAfterRateChange(newRate)
    }

    private fun rederiveAfterRateChange(newRate: BigDecimal) {
        when (primary.value) {
            UpiOfframpAmountSide.USDC -> {
                usdcState.value.amount?.let { usdc ->
                    inrState.update {
                        NumberTextFieldInnerState.fromAmount(
                            usdc.multiply(newRate).setScale(INR_INPUT_SCALE, RoundingMode.FLOOR),
                        )
                    }
                }
            }

            UpiOfframpAmountSide.INR -> {
                inrState.value.amount?.let { inr ->
                    usdcState.update {
                        NumberTextFieldInnerState.fromAmount(
                            inr.divide(newRate, USDC_INPUT_SCALE, RoundingMode.FLOOR),
                        )
                    }
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
        balance: Usdc6?,
    ): UpiOfframpState {
        val usdcAmount = usdc.amount
        val validationError =
            if (inFlightCheckpoint != null) {
                stringRes(R.string.upi_offramp_error_in_flight)
            } else {
                validate(usdcAmount, upi)
            }
        val rateDisplay = currentRate.stripTrailingZeros().toPlainString()
        val sendButtonText =
            if (inFlightCheckpoint != null) {
                stringRes(R.string.upi_offramp_resume_button)
            } else {
                stringRes(R.string.upi_offramp_send_button)
            }
        val sendEnabled =
            if (inFlightCheckpoint != null) {
                true
            } else {
                validationError == null &&
                    usdcAmount != null &&
                    usdcAmount > BigDecimal.ZERO &&
                    upi.isNotBlank() &&
                    UpiQrParser.validateUpiId(upi)
            }
        val amountValid =
            inFlightCheckpoint == null &&
                validationError == null &&
                usdcAmount != null &&
                usdcAmount > BigDecimal.ZERO
        val orderAmount: Usdc6? = if (amountValid) Usdc6.ofWhole(usdcAmount) else null
        return UpiOfframpState(
            primary = side,
            usdcInput = NumberTextFieldState(innerState = usdc, onValueChange = ::onUsdcChange),
            inrInput = NumberTextFieldState(innerState = inr, onValueChange = ::onInrChange),
            onSwapSides = ::onSwapSides,
            rateText = stringRes(R.string.upi_offramp_rate_label, rateDisplay),
            upiField = TextFieldState(stringRes(upi)) { newValue -> onUpiChange(newValue) },
            infoText = if (amountValid) stringRes(R.string.upi_offramp_estimate_disclaimer) else null,
            errorText = validationError,
            sendButton =
                ButtonState(
                    text = sendButtonText,
                    isEnabled = sendEnabled,
                    onClick = ::onSendClick,
                ),
            onScanQr = ::onScanQr,
            baseBalanceText =
                balance?.let {
                    stringRes(R.string.upi_offramp_base_balance_label, it.toDisplayString(stripTrailingZeros = true))
                },
            fundingPlanText =
                if (orderAmount != null && balance != null) {
                    fundingPlanText(orderAmount = orderAmount, balance = balance)
                } else {
                    null
                },
            onDiscardInFlight = if (inFlightCheckpoint != null) ::onDiscardInFlight else null,
        )
    }

    /**
     * The plain-English funding plan shown below the amount inputs. Honors the existing
     * "bridge the full order amount when balance is short" behavior (we don't bridge just the delta),
     * and on testnet replaces the NEAR bridge copy with a manual-fund hint since no bridge runs there.
     */
    private fun fundingPlanText(orderAmount: Usdc6, balance: Usdc6): StringResource {
        if (balance >= orderAmount) return stringRes(R.string.upi_offramp_funding_from_base)
        val orderDisplay = orderAmount.toDisplayString(stripTrailingZeros = true)
        return if (network.chainId == P2pNetworks.MAINNET_CHAIN_ID) {
            stringRes(R.string.upi_offramp_funding_via_near, orderDisplay)
        } else {
            stringRes(R.string.upi_offramp_funding_need_manual, orderDisplay)
        }
    }

    private fun onDiscardInFlight() {
        viewModelScope.launch { checkpointStorage.clear() }
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
                // QR carried an `am=` field; treat INR as the user's primary so USDC re-derives
                // from the live rate (a scanned amount must not lock the USDC field).
                primary.update { UpiOfframpAmountSide.INR }
                inrState.update {
                    NumberTextFieldInnerState.fromAmount(fiat.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR))
                }
                val currentRate = rate.value
                usdcState.update {
                    NumberTextFieldInnerState.fromAmount(
                        fiat.divide(currentRate, USDC_INPUT_SCALE, RoundingMode.FLOOR),
                    )
                }
            }
        }
    }

    private fun onUsdcChange(next: NumberTextFieldInnerState) {
        primary.update { UpiOfframpAmountSide.USDC }
        usdcState.update { next }
        val currentRate = rate.value
        val derivedInr = next.amount?.multiply(currentRate)?.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR)
        inrState.update {
            if (derivedInr == null) NumberTextFieldInnerState() else NumberTextFieldInnerState.fromAmount(derivedInr)
        }
    }

    private fun onInrChange(next: NumberTextFieldInnerState) {
        primary.update { UpiOfframpAmountSide.INR }
        inrState.update { next }
        val currentRate = rate.value
        val derivedUsdc = next.amount?.divide(currentRate, USDC_INPUT_SCALE, RoundingMode.FLOOR)
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
        val rawUsdc = usdcState.value.amount ?: return
        val rawInr = inrState.value.amount ?: return
        if (rawUsdc <= BigDecimal.ZERO || rawUsdc > USDC_CAP) return
        if (rawInr <= BigDecimal.ZERO) return
        val upi = upiText.value
        if (upi.isBlank() || !UpiQrParser.validateUpiId(upi)) return
        // Snap to URI/Diamond-aligned precisions before placing. The URI's am= field is 2dp; the
        // Diamond re-derives USDC from am= at setSellOrderUpi and atomically cancels if
        // updatedAmount ≠ floor(am × 1e6 / sellPrice). Re-deriving USDC here from snapped INR
        // guarantees that placement, parsedUsdcMicros, and the Diamond's check all agree.
        val snappedInr = rawInr.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR)
        val alignedUsdc = snappedInr.divide(rate.value, USDC_INPUT_SCALE, RoundingMode.FLOOR)
        val usdcMicro = Usdc6.ofWhole(alignedUsdc).micros
        val fiatMicro = Usdc6.ofWhole(snappedInr).micros
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

        // Used until getPriceConfig returns. ₹85/USDC is the p2p.me historical default.
        private val FALLBACK_RATE: BigDecimal = BigDecimal("85")

        // p2p.me caps a single offramp at 100 USDC. Surfaced proactively in the UI via
        // R.string.upi_offramp_limit_hint and enforced here as a hard input cap.
        private val USDC_CAP: BigDecimal = BigDecimal("100")

        // 6dp so a 2dp INR amount can be expressed as the EXACT USDC the Diamond derives from am=
        // (Diamond reads am= as a 2dp-floored string from the URI; updatedAmount must equal
        // floor(am × 1e6 / sellPrice). Truncating USDC display would mask the alignment).
        private const val USDC_INPUT_SCALE = 6

        // 2dp matches UpiPayUri's am= encoding. Anything more precise is silently floored on-chain
        // and causes setSellOrderUpi to atomically cancel (parsed-from-am ≠ updatedAmount).
        private const val INR_INPUT_SCALE = 2
        private const val RATE_REFRESH_INTERVAL_MS = 30_000L
        private const val BALANCE_REFRESH_INTERVAL_MS = 30_000L
    }
}

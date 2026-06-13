package co.electriccoin.zcash.ui.screen.swap.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProvider
import co.electriccoin.zcash.ui.common.provider.StoreCorruptedException
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanUpiUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.TextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.settings.p2p.P2pTransactionsArgs
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
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
    private val inrState = MutableStateFlow(NumberTextFieldInnerState())
    private val upiText = MutableStateFlow("")
    private val rate = MutableStateFlow(FALLBACK_RATE)
    private val inFlight = MutableStateFlow<OfframpCheckpoint?>(null)
    private val baseBalance = MutableStateFlow<Usdc6?>(null)

    // Deterministic from the owner key, so resolve once. Null until the first factory call returns.
    private var smartAccountAddress: Address? = null

    // Driven by onStart/onCompletion on the state flow; the rate/balance pollers run only while
    // this is > 0, so a backgrounded screen doesn't burn RPC quota.
    private val activeSubscribers = MutableStateFlow(0)

    val state: StateFlow<UpiOfframpState> =
        combine(
            inrState,
            upiText,
            rate,
            inFlight,
            baseBalance,
        ) { inr, upi, currentRate, checkpoint, balance ->
            buildState(
                inr = inr,
                upi = upi,
                currentRate = currentRate,
                inFlightCheckpoint = checkpoint,
                balance = balance,
            )
        }
            .onStart { activeSubscribers.update { it + 1 } }
            .onCompletion { activeSubscribers.update { (it - 1).coerceAtLeast(0) } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue =
                    buildState(
                        inr = inrState.value,
                        upi = upiText.value,
                        currentRate = rate.value,
                        inFlightCheckpoint = inFlight.value,
                        balance = baseBalance.value,
                    ),
            )

    init {
        viewModelScope.launch {
            checkpointStorage.observe()
                .catch { e ->
                    if (e is StoreCorruptedException) {
                        Twig.warn(e) { "UpiOfframpVM: corrupted checkpoint blob, discarding" }
                        checkpointStorage.clear()
                        emit(null)
                    } else {
                        throw e
                    }
                }
                .collect { checkpoint -> inFlight.update { checkpoint } }
        }
        viewModelScope.launch {
            activeSubscribers
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { isSubscribed ->
                    if (!isSubscribed) return@collectLatest
                    pollRate()
                }
        }
        viewModelScope.launch {
            activeSubscribers
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { isSubscribed ->
                    if (!isSubscribed) return@collectLatest
                    pollBalance()
                }
        }
    }

    // §5f: refetch every 30s so the quote tracks the rate the contract will stamp.
    private suspend fun pollRate() =
        coroutineScope {
            while (isActive) {
                refreshRate()
                delay(RATE_REFRESH_INTERVAL_MS)
            }
        }

    // Surfaces the Base USDC balance (reusable from prior cancelled orders). Resolves the smart
    // account lazily on first subscription; a provider-level cache keeps repeat resolves cheap.
    private suspend fun pollBalance() =
        coroutineScope {
            if (smartAccountAddress == null) {
                smartAccountAddress =
                    runCatching { accountProvider.resolve().address }
                        .onFailure { Twig.warn(it) { "UpiOfframpVM: smart account resolve failed" } }
                        .getOrNull()
            }
            if (smartAccountAddress == null) return@coroutineScope
            while (isActive) {
                refreshBaseBalance()
                delay(BALANCE_REFRESH_INTERVAL_MS)
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
    }

    private fun buildState(
        inr: NumberTextFieldInnerState,
        upi: String,
        currentRate: BigDecimal,
        inFlightCheckpoint: OfframpCheckpoint?,
        balance: Usdc6?,
    ): UpiOfframpState {
        // INR is the source of truth; USDC re-derives at the placed precision (2dp-snapped INR,
        // matching onSendClick) and nulls out a sub-micro amount that floors to 0 USDC.
        val usdcAmount: BigDecimal? =
            inr.amount
                ?.takeIf { it > BigDecimal.ZERO }
                ?.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR)
                ?.divide(currentRate, USDC_INPUT_SCALE, RoundingMode.FLOOR)
                ?.takeIf { it > BigDecimal.ZERO }
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
                    upi.isNotBlank() &&
                    UpiQrParser.validateUpiId(upi)
            }
        val orderAmount: Usdc6? =
            if (inFlightCheckpoint == null && validationError == null && usdcAmount != null) {
                Usdc6.ofWhole(usdcAmount)
            } else {
                null
            }
        return UpiOfframpState(
            onScanQr = ::onScanQr,
            upiField = TextFieldState(stringRes(upi)) { newValue -> onUpiChange(newValue) },
            inrInput = NumberTextFieldState(innerState = inr, onValueChange = ::onInrChange),
            usdcEquivalent =
                usdcAmount?.let {
                    stringRes(
                        R.string.upi_offramp_usdc_equivalent,
                        Usdc6.ofWhole(it).toDisplayString(stripTrailingZeros = true),
                    )
                },
            rateText = stringRes(R.string.upi_offramp_rate_label, rateDisplay),
            infoText = if (orderAmount != null) stringRes(R.string.upi_offramp_estimate_disclaimer) else null,
            errorText = validationError,
            sendButton =
                ButtonState(
                    text = sendButtonText,
                    isEnabled = sendEnabled,
                    onClick = ::onSendClick,
                ),
            onHistoryClick = ::onHistoryClick,
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

    // Bridges the full order amount when the Base balance is short (not just the delta). Testnet has
    // no NEAR bridge, so it shows a manual-fund hint instead.
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

    private fun onHistoryClick() = navigationRouter.forward(P2pTransactionsArgs)

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
                // QR carried an `am=` field (dynamic merchant QR): prefill INR; USDC re-derives in buildState.
                inrState.update {
                    NumberTextFieldInnerState.fromAmount(fiat.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR))
                }
            }
        }
    }

    private fun onInrChange(next: NumberTextFieldInnerState) {
        inrState.update { next }
    }

    private fun onUpiChange(next: String) {
        upiText.update { next.trim() }
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
        val rawInr = inrState.value.amount ?: return
        if (rawInr <= BigDecimal.ZERO) return
        val upi = upiText.value
        if (upi.isBlank() || !UpiQrParser.validateUpiId(upi)) return
        // Snap INR to 2dp and re-derive USDC, so the placed amount matches what the Diamond derives
        // from the URI's am= field — a mismatch makes setSellOrderUpi atomically cancel the order.
        val snappedInr = rawInr.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR)
        val alignedUsdc = snappedInr.divide(rate.value, USDC_INPUT_SCALE, RoundingMode.FLOOR)
        if (alignedUsdc <= BigDecimal.ZERO || alignedUsdc > USDC_CAP) return
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

        private const val USDC_INPUT_SCALE = 6

        // 2dp matches UpiPayUri's am= encoding; more precision is floored on-chain (see onSendClick).
        private const val INR_INPUT_SCALE = 2
        private const val RATE_REFRESH_INTERVAL_MS = 30_000L
        private const val BALANCE_REFRESH_INTERVAL_MS = 30_000L
    }
}

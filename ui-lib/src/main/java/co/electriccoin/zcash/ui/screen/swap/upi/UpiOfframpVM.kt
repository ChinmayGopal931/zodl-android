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
import co.electriccoin.zcash.ui.design.component.zapp.ZappConfirmationState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.settings.p2p.P2pTransactionsArgs
import co.electriccoin.zcash.ui.screen.swap.upi.bridge.BridgeToBaseArgs
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpProgressArgs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
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
import java.math.BigInteger
import java.math.RoundingMode

@Suppress("TooManyFunctions")
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

    // Re-quote confirmation sheet, surfaced separately from [state] (like SwapVM.cancelState) so the
    // main combine stays at 5 flows. Null = hidden.
    private val payConfirmationState = MutableStateFlow<ZappConfirmationState?>(null)
    val payConfirmation: StateFlow<ZappConfirmationState?> = payConfirmationState.asStateFlow()

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
        val isShortOnMainnet =
            inFlightCheckpoint == null &&
                usdcAmount != null &&
                balance != null &&
                balance < Usdc6.ofWhole(usdcAmount) &&
                network.chainId == P2pNetworks.MAINNET_CHAIN_ID
        val sendButtonText =
            when {
                inFlightCheckpoint != null -> stringRes(R.string.upi_offramp_resume_button)
                isShortOnMainnet -> stringRes(R.string.upi_offramp_pay_button_add_funds)
                else -> stringRes(R.string.upi_offramp_send_button)
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
            onAddFunds = ::onAddFunds,
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

    // Funded → pay straight from Base. Short on mainnet → hint the top-up step (the shortfall is
    // bridged first). Short on testnet → manual-fund hint (no NEAR route).
    private fun fundingPlanText(orderAmount: Usdc6, balance: Usdc6): StringResource {
        if (balance >= orderAmount) return stringRes(R.string.upi_offramp_funding_from_base)
        return if (network.chainId == P2pNetworks.MAINNET_CHAIN_ID) {
            stringRes(
                R.string.upi_offramp_funding_topup_first,
                topUpShortfall(orderAmount, balance).toDisplayString(stripTrailingZeros = true),
            )
        } else {
            stringRes(R.string.upi_offramp_funding_need_manual, orderAmount.toDisplayString(stripTrailingZeros = true))
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
        inFlight.value?.let { existing ->
            navigationRouter.forward(resumeArgs(existing))
            return
        }
        val rawInr = inrState.value.amount ?: return
        if (rawInr <= BigDecimal.ZERO) return
        val upi = upiText.value
        if (upi.isBlank() || !UpiQrParser.validateUpiId(upi)) return
        viewModelScope.launch { reQuoteAndRoute(rawInr, upi) }
    }

    // Re-quote: the sell rate the contract stamps can drift, so refetch it the instant the user
    // commits, recompute the USDC, then either confirm a payment from the Base balance or route to a
    // top-up bridge when the balance is short. The pay flow never kicks off an inline bridge itself.
    private suspend fun reQuoteAndRoute(rawInr: BigDecimal, upi: String) {
        val freshRate = refreshRateNow()
        // Snap INR to 2dp and re-derive USDC, so the placed amount matches what the Diamond derives
        // from the URI's am= field — a mismatch makes setSellOrderUpi atomically cancel the order.
        val snappedInr = rawInr.setScale(INR_INPUT_SCALE, RoundingMode.FLOOR)
        val alignedUsdc = snappedInr.divide(freshRate, USDC_INPUT_SCALE, RoundingMode.FLOOR)
        if (alignedUsdc <= BigDecimal.ZERO || alignedUsdc > USDC_CAP) return
        val requiredUsdc = Usdc6.ofWhole(alignedUsdc)
        val fiatMicro = Usdc6.ofWhole(snappedInr).micros
        val balance = refreshBaseBalanceNow()
        when {
            balance != null && balance >= requiredUsdc ->
                showPayConfirmation(snappedInr, requiredUsdc, freshRate, upi, fiatMicro)

            network.chainId == P2pNetworks.MAINNET_CHAIN_ID ->
                navigationRouter.forward(
                    BridgeToBaseArgs(prefillUsdcMicro = topUpShortfall(requiredUsdc, balance).micros.toString()),
                )

            // Testnet has no bridge; let the order flow surface PreFundedOfframpFunding's manual-fund guidance.
            else -> navigationRouter.forward(progressArgs(upi, requiredUsdc.micros, fiatMicro))
        }
    }

    private fun showPayConfirmation(
        inr: BigDecimal,
        usdc: Usdc6,
        currentRate: BigDecimal,
        upi: String,
        fiatMicro: BigInteger,
    ) {
        payConfirmationState.update {
            ZappConfirmationState(
                title = stringRes(R.string.upi_offramp_confirm_title),
                message =
                    stringRes(
                        R.string.upi_offramp_confirm_message,
                        inr.toPlainString(),
                        usdc.toDisplayString(stripTrailingZeros = true),
                        currentRate.stripTrailingZeros().toPlainString(),
                    ),
                primaryButton =
                    ButtonState(
                        text = stringRes(R.string.upi_offramp_confirm_pay),
                        onClick = {
                            payConfirmationState.update { null }
                            navigationRouter.forward(progressArgs(upi, usdc.micros, fiatMicro))
                        },
                    ),
                secondaryButton =
                    ButtonState(
                        text = stringRes(R.string.upi_offramp_confirm_cancel),
                        onClick = { payConfirmationState.update { null } },
                    ),
                onBack = { payConfirmationState.update { null } },
            )
        }
    }

    private fun onAddFunds() = navigationRouter.forward(BridgeToBaseArgs())

    private fun progressArgs(upi: String, usdcMicro: BigInteger, fiatMicro: BigInteger) =
        UpiOfframpProgressArgs(
            recipientUpi = upi,
            usdcAmountMicro = usdcMicro.toString(),
            fiatAmountMicro = fiatMicro.toString(),
            currency = CURRENCY,
        )

    private fun resumeArgs(existing: OfframpCheckpoint) =
        UpiOfframpProgressArgs(
            recipientUpi = existing.recipientUpi,
            usdcAmountMicro = existing.usdcAmountMicroDecimal,
            // Old checkpoints lack fiat — orchestrator resolves a fallback at resume time.
            fiatAmountMicro = existing.fiatAmountMicroDecimal ?: existing.usdcAmountMicroDecimal,
            payeeName = existing.payeeName,
            currency = existing.currency,
        )

    private fun topUpShortfall(required: Usdc6, balance: Usdc6?): Usdc6 {
        val have = balance?.micros ?: BigInteger.ZERO
        val shortMicros = (required.micros - have).max(BigInteger.ONE)
        // Round the prefill up to the nearest 0.01 USDC so it comfortably covers the order amount.
        val step = USDC_TOPUP_ROUNDING_MICROS
        return Usdc6(((shortMicros + step - BigInteger.ONE) / step) * step)
    }

    private suspend fun refreshRateNow(): BigDecimal {
        runCatching { rpc.getPriceConfig(network.diamondAddress, CURRENCY).sellPriceAsRate() }
            .onFailure { Twig.warn(it) { "UpiOfframpVM: re-quote getPriceConfig failed" } }
            .getOrNull()
            ?.let { fresh -> rate.update { fresh } }
        return rate.value
    }

    private suspend fun refreshBaseBalanceNow(): Usdc6? {
        val account =
            smartAccountAddress
                ?: runCatching { accountProvider.resolve().address }.getOrNull()?.also { smartAccountAddress = it }
                ?: return baseBalance.value
        val fetched =
            runCatching { rpc.getUsdcBalance(network.usdcAddress, account) }
                .onFailure { Twig.warn(it) { "UpiOfframpVM: re-quote getUsdcBalance failed" } }
                .getOrNull()
        if (fetched != null) baseBalance.update { fetched }
        return fetched ?: baseBalance.value
    }

    companion object {
        private val CURRENCY = CurrencyCode.Inr

        // Used until getPriceConfig returns. ₹85/USDC is the p2p.me historical default.
        private val FALLBACK_RATE: BigDecimal = BigDecimal("85")

        // p2p.me caps a single offramp at 100 USDC. Surfaced proactively in the UI via
        // R.string.upi_offramp_limit_hint and enforced here as a hard input cap.
        private val USDC_CAP: BigDecimal = BigDecimal("100")

        // Round a prefilled top-up amount up to the nearest 0.01 USDC (10_000 micros).
        private val USDC_TOPUP_ROUNDING_MICROS: BigInteger = BigInteger.valueOf(10_000)

        private const val USDC_INPUT_SCALE = 6

        // 2dp matches UpiPayUri's am= encoding; more precision is floored on-chain (see onSendClick).
        private const val INR_INPUT_SCALE = 2
        private const val RATE_REFRESH_INTERVAL_MS = 30_000L
        private const val BALANCE_REFRESH_INTERVAL_MS = 30_000L
    }
}

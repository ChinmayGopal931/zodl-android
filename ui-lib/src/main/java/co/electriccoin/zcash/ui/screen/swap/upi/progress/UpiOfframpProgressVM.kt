package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.provider.OfframpCheckpointStorageProvider
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.offramp.account.SmartOfframpAccountProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.orchestrator.KnownRevertReason
import xyz.justzappit.offramp.orchestrator.OfframpDriver
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import xyz.justzappit.offramp.orchestrator.orderId
import xyz.justzappit.offramp.orchestrator.step
import xyz.justzappit.offramp.p2p.OrderFeeDetails
import xyz.justzappit.offramp.p2p.Usdc6
import xyz.justzappit.offramp.p2p.getAdditionalOrderDetails
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("TooManyFunctions")
internal class UpiOfframpProgressVM(
    private val args: UpiOfframpProgressArgs,
    private val orchestrator: OfframpDriver,
    private val network: P2pNetworkConfig,
    private val accountProvider: SmartOfframpAccountProvider,
    private val navigationRouter: NavigationRouter,
    private val checkpointStorage: OfframpCheckpointStorageProvider,
    private val rpc: BaseRpcClient,
) : ViewModel() {
    private val request: OfframpRequest =
        OfframpRequest(
            recipientUpi = args.recipientUpi,
            usdcAmount = Usdc6(BigInteger(args.usdcAmountMicro)),
            fiatAmount = Usdc6(BigInteger(args.fiatAmountMicro)),
            payeeName = args.payeeName,
            currency = args.currency,
            // 1% floor against rate drift across the funding bridge (§5e of findings).
            fiatAmountLimit = computeFiatAmountLimit(args.fiatAmountMicro),
        )

    private val persister = OfframpCheckpointPersister(storage = checkpointStorage, request = request)

    private val feeDetails = MutableStateFlow<OrderFeeDetails?>(null)

    // The on-chain identity shown to the user: the ERC-4337 smart account, not the owner EOA.
    // Resolved once via an async factory call (see init); null until then.
    private val smartAccountAddress = MutableStateFlow<Address?>(null)

    // Sticky: once the funding seam short-circuits with FundedFromBase, the FUNDING progress row
    // keeps that label for the rest of the run. Live status moves on once the next step begins, so
    // a non-sticky reading would flip the label back to "Bridging funds" by the time the user sees it.
    private val fundedFromBaseObserved = MutableStateFlow(false)

    /**
     * Full history of statuses emitted by this run, in order. The list lets late subscribers (e.g.
     * a rotation after WaitingForMerchantAcceptance has fired) reconstruct what happened earlier
     * — the previous design used `shareIn(Eagerly, replay = 1)`, which could silently drop the
     * entire Idle → SelectingCircle → ... → WaitingForX prefix if subscription was delayed even
     * one frame, leaving the UI looking like the flow jumped straight to the latest step.
     *
     * The orchestrator's cold Flow is collected exactly once (in `init`); every emission appends
     * here and runs the same side effects (persister, Twig, fundedFromBaseObserved). Downstream
     * consumers (`state` combine + fee-details listener) read from `statusList`, never from the
     * raw orchestrator Flow.
     */
    private val statusList = MutableStateFlow<List<OfframpStatus>>(emptyList())

    init {
        // Drive the orchestrator. Single collector, full history captured, side effects co-located.
        viewModelScope.launch {
            val existing = checkpointStorage.get()
            // Resume whenever there's an order already placed OR a funding bridge in flight: the
            // bridge's persisted 1-Click deposit address must be re-polled, never re-quoted, or a
            // crash mid-bridge would open a second bridge and double-send the user's ZEC. Only a
            // checkpoint with neither is empty noise worth discarding.
            val upstream =
                if (existing != null && (existing.orderIdBig != null || existing.bridgeDepositAddress != null)) {
                    Twig.info {
                        "UpiOfframpProgress resuming from ${existing.currentStep} " +
                            "(orderId=${existing.orderId}, bridge=${existing.bridgeDepositAddress != null})"
                    }
                    persister.seedFrom(existing)
                    orchestrator.resume(existing)
                } else {
                    if (existing != null) {
                        Twig.warn { "UpiOfframpProgress: discarding empty checkpoint at ${existing.currentStep}" }
                        checkpointStorage.clear()
                    }
                    orchestrator.run(request)
                }
            upstream.collect { status ->
                Twig.info { "UpiOfframpProgress status=$status" }
                persister.onStatus(status)
                if (status is OfframpStatus.FundedFromBase) fundedFromBaseObserved.update { true }
                statusList.update { it + status }
            }
        }

        viewModelScope.launch {
            runCatching { accountProvider.resolve().address }
                .onSuccess { addr -> smartAccountAddress.update { addr } }
                .onFailure { Twig.warn(it) { "UpiOfframpProgress: failed to resolve smart account address" } }
        }

        // Fee details: refetch whenever orderId or status-class changes. distinctUntilChanged
        // throttles the WaitingForCompletion poll loop (which emits every 3s) down to one fetch
        // per genuine state transition.
        viewModelScope.launch {
            statusList
                .mapNotNull { list ->
                    val last = list.lastOrNull() ?: return@mapNotNull null
                    last.orderId?.let { it to last::class }
                }.distinctUntilChanged()
                .collect { (orderId, _) ->
                    runCatching { rpc.getAdditionalOrderDetails(network.diamondAddress, orderId) }
                        .onSuccess { details -> feeDetails.update { details } }
                        .onFailure { Twig.warn(it) { "UpiOfframpProgress: getAdditionalOrderDetails($orderId) failed" } }
                }
        }
    }

    val state: StateFlow<UpiOfframpProgressState> =
        combine(statusList, feeDetails, smartAccountAddress, fundedFromBaseObserved) { list, fees, addr, fundedFromBase ->
            buildState(list.lastOrNull() ?: OfframpStatus.Idle, fees, addr, fundedFromBase)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildState(OfframpStatus.Idle, null, null, false),
        )

    private fun buildState(
        status: OfframpStatus,
        fees: OrderFeeDetails?,
        accountAddress: Address?,
        fundedFromBase: Boolean,
    ): UpiOfframpProgressState {
        val orderId = status.orderId
        val summary = buildSummary(status, orderId, accountAddress)

        val title =
            when (status) {
                is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_title_completed)
                is OfframpStatus.Cancelled -> stringRes(R.string.upi_offramp_progress_title_cancelled)
                is OfframpStatus.Failed -> stringRes(R.string.upi_offramp_progress_title_failed)
                else -> stringRes(R.string.upi_offramp_progress_title_in_progress)
            }

        val subtitle: StringResource? =
            when (status) {
                is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_subtitle_completed)
                is OfframpStatus.Cancelled -> stringRes(R.string.upi_offramp_progress_subtitle_cancelled)
                is OfframpStatus.Failed -> null
                else -> stringRes(R.string.upi_offramp_progress_subtitle_recipient, args.recipientUpi)
            }

        val steps = buildProgressSteps(status, network, fundedFromBaseObserved = fundedFromBase)
        val failure = (status as? OfframpStatus.Failed)?.let(::buildFailureCard)
        val cancelled = (status as? OfframpStatus.Cancelled)?.let(::buildCancelledCard)
        val feeBreakdown = buildFeeBreakdown(fees)

        // Refund-back-to-ZEC is reachable only from Settings → P2P transactions, never inline here.
        // Keeps the in-flow primary action a single "done / close" once the order is terminal.
        return UpiOfframpProgressState(
            title = title,
            subtitle = subtitle,
            summary = summary,
            feeBreakdown = feeBreakdown,
            steps = steps,
            failure = failure,
            cancelled = cancelled,
            primaryButton = primaryButtonFor(status),
            onBack = { navigationRouter.back() },
        )
    }

    private fun primaryButtonFor(status: OfframpStatus): ButtonState? =
        when (status) {
            is OfframpStatus.Completed -> {
                ButtonState(
                    text = stringRes(R.string.upi_offramp_progress_done_button),
                    onClick = { navigationRouter.back() },
                )
            }

            is OfframpStatus.Cancelled, is OfframpStatus.Failed -> {
                ButtonState(
                    text = stringRes(R.string.upi_offramp_progress_close_button),
                    onClick = { navigationRouter.back() },
                )
            }

            else -> {
                null
            }
        }

    private fun buildSummary(
        status: OfframpStatus,
        orderId: BigInteger?,
        accountAddress: Address?,
    ): UpiOfframpOrderSummary {
        val completionDuration =
            (status as? OfframpStatus.Completed)?.let {
                completionDurationString(it.placedAtEpochSeconds, it.completedAtEpochSeconds)
            }
        val terminalTimestamp =
            when (status) {
                is OfframpStatus.Completed -> status.completedAtEpochSeconds?.let(::formatTerminalTimestamp)
                is OfframpStatus.Cancelled -> status.cancelledAtEpochSeconds?.let(::formatTerminalTimestamp)
                else -> null
            }
        val merchantAddress =
            when (status) {
                is OfframpStatus.Completed -> status.acceptedMerchant.checksumHex
                is OfframpStatus.Cancelled -> status.acceptedMerchant?.checksumHex
                else -> null
            }
        return UpiOfframpOrderSummary(
            amountUsdcDisplay =
                stringRes(
                    R.string.upi_offramp_progress_amount_usdc,
                    runCatching { Usdc6(BigInteger(args.usdcAmountMicro)).toDisplayString() }.getOrDefault("0"),
                ),
            recipient = args.recipientUpi,
            orderId = orderId?.toString(),
            networkName = network.name.replaceFirstChar { it.uppercase(Locale.ROOT) },
            signerAddress = accountAddress?.checksumHex,
            signerExplorerUrl = accountAddress?.let { network.addressUrl(it.checksumHex) },
            completionDuration = completionDuration,
            terminalTimestamp = terminalTimestamp,
            merchantAddress = merchantAddress,
            merchantExplorerUrl = merchantAddress?.let { network.addressUrl(it) },
        )
    }

    private fun buildFeeBreakdown(fees: OrderFeeDetails?): UpiOfframpFeeBreakdown? {
        if (fees == null) return null
        // Pre-acceptance the contract returns all-zeros; rendering that gives a misleading
        // "fee = 0.00 USDC" line. Only surface the card once at least one value is meaningful.
        if (fees.fixedFeePaid == Usdc6.ZERO && fees.actualUsdcAmount == Usdc6.ZERO) return null
        val youSend =
            fees.actualUsdcAmount
                .takeIf { it > Usdc6.ZERO }
                ?.let { stringRes(R.string.upi_offramp_fee_breakdown_amount_usdc, displayUsdc(it)) }
        val fee =
            fees.fixedFeePaid
                .takeIf { it > Usdc6.ZERO }
                ?.let { stringRes(R.string.upi_offramp_fee_breakdown_amount_usdc, displayUsdc(it)) }
        val youReceive =
            fees.actualFiatAmount
                .takeIf { it > Usdc6.ZERO }
                ?.let { stringRes(R.string.upi_offramp_fee_breakdown_amount_inr, displayUsdc(it)) }
        return UpiOfframpFeeBreakdown(youSend = youSend, fee = fee, youReceive = youReceive)
    }

    private fun buildCancelledCard(cancelled: OfframpStatus.Cancelled): UpiOfframpCancelledCard {
        // Where the reclaimed funds went: mainnet pulls USDC back to ZEC (NEAR), testnet leaves it in
        // the self-custodial account (no NEAR route).
        val returnsToZec = network.chainId == ChainId.BASE_MAINNET
        val refundedAmount =
            cancelled.refundedUsdcAmount?.let {
                val res =
                    if (returnsToZec) {
                        R.string.upi_offramp_cancelled_returned_zec
                    } else {
                        R.string.upi_offramp_cancelled_refunded
                    }
                stringRes(res, displayUsdc(it))
            }
        val cancelledAt =
            cancelled.cancelledAtEpochSeconds?.let {
                stringRes(R.string.upi_offramp_cancelled_at, formatTerminalTimestampValue(it))
            }
        return UpiOfframpCancelledCard(
            refundedAmount = refundedAmount,
            cancelledAt = cancelledAt,
            tip = stringRes(R.string.upi_offramp_cancelled_tip),
        )
    }

    private fun buildFailureCard(failed: OfframpStatus.Failed): UpiOfframpFailureCard {
        val rawForUi = failed.message.take(MAX_RAW_MESSAGE_LEN)
        val txHashHex = failed.txHash?.hex
        return UpiOfframpFailureCard(
            stepLabel = stringRes(stepLabelRes(failed.step)),
            decodedReason = decodedReason(failed),
            rawSelector = failed.revertSelector?.hex,
            rawMessage = rawForUi,
            txHash = txHashHex,
            txExplorerUrl = txHashHex?.let { network.txUrl(it) },
        )
    }

    private fun decodedReason(failed: OfframpStatus.Failed): StringResource? {
        failed.knownRevertReason?.let { return stringRes(curatedRevertStringRes(it)) }
        (failed.sdkErrorMessage ?: failed.sdkErrorName)?.let {
            return stringRes(R.string.upi_offramp_revert_sdk_long_tail, it)
        }
        return failed.solidityErrorString?.let(::stringRes)
    }

    private fun curatedRevertStringRes(reason: KnownRevertReason): Int =
        when (reason) {
            KnownRevertReason.BuyOrderAmountExceedsLimit -> R.string.upi_offramp_revert_buy_order_amount_exceeds_limit
            KnownRevertReason.InsufficientReputation -> R.string.upi_offramp_revert_insufficient_reputation
            KnownRevertReason.OrderAmountExceedsLimit -> R.string.upi_offramp_revert_order_amount_exceeds_limit
            KnownRevertReason.SellAmountExceedsFiatLimit -> R.string.upi_offramp_revert_sell_amount_exceeds_fiat_limit
            KnownRevertReason.CurrencyNotSupported -> R.string.upi_offramp_revert_currency_not_supported
            KnownRevertReason.UserIsBlacklisted -> R.string.upi_offramp_revert_user_is_blacklisted
            KnownRevertReason.ExchangeNotOperational -> R.string.upi_offramp_revert_exchange_not_operational
            KnownRevertReason.NotEnoughEligibleMerchants -> R.string.upi_offramp_revert_not_enough_eligible_merchants
            KnownRevertReason.OrderExpired -> R.string.upi_offramp_revert_order_expired
            KnownRevertReason.UpiAlreadySent -> R.string.upi_offramp_revert_upi_already_sent
            KnownRevertReason.InvalidOrderUpi -> R.string.upi_offramp_revert_invalid_order_upi
            KnownRevertReason.OrderNotAccepted -> R.string.upi_offramp_revert_order_not_accepted
            KnownRevertReason.UsdcTransferFailed -> R.string.upi_offramp_revert_usdc_transfer_failed
            KnownRevertReason.NotAuthorized -> R.string.upi_offramp_revert_not_authorized
        }

    private fun completionDurationString(placedSec: Long?, completedSec: Long?): StringResource? {
        if (placedSec == null || completedSec == null || completedSec <= placedSec) return null
        val totalSeconds = completedSec - placedSec
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        val text = if (minutes == 0L) "${seconds}s" else "${minutes}m${seconds}s"
        return stringRes(R.string.upi_offramp_completion_duration, text)
    }

    private fun formatTerminalTimestamp(epochSeconds: Long): StringResource =
        stringRes(R.string.upi_offramp_terminal_at, formatTerminalTimestampValue(epochSeconds))

    private fun formatTerminalTimestampValue(epochSeconds: Long): String =
        terminalDateFormat.format(Date(epochSeconds * MILLIS_PER_SECOND))

    private fun displayUsdc(value: Usdc6): String = value.toDisplayString()

    companion object {
        private const val MAX_RAW_MESSAGE_LEN = 500
        private const val SECONDS_PER_MINUTE = 60L
        private const val MILLIS_PER_SECOND = 1_000L

        private val SLIPPAGE_FLOOR_BASIS_POINTS = BigInteger.valueOf(9_900)
        private val BASIS_POINTS_DENOMINATOR = BigInteger.valueOf(10_000)

        private fun computeFiatAmountLimit(fiatAmountMicro: String): Usdc6? {
            val micros = runCatching { BigInteger(fiatAmountMicro) }.getOrNull() ?: return null
            if (micros.signum() <= 0) return null
            return Usdc6(micros.multiply(SLIPPAGE_FLOOR_BASIS_POINTS).divide(BASIS_POINTS_DENOMINATOR))
        }

        // Locale-stable format so screenshots / fixtures don't drift across devices.
        private val terminalDateFormat: SimpleDateFormat =
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
    }
}

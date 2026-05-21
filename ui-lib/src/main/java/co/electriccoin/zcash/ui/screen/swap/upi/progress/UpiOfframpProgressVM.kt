package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.OfframpRepository
import co.electriccoin.zcash.ui.common.usecase.GetOrderFeeDetailsUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.orchestrator.KnownRevertReason
import xyz.justzappit.offramp.orchestrator.OfframpDriver
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import xyz.justzappit.offramp.orchestrator.OfframpStep
import xyz.justzappit.offramp.orchestrator.orderId
import xyz.justzappit.offramp.orchestrator.step
import xyz.justzappit.offramp.p2p.OrderFeeDetails
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigDecimal
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
    private val account: EvmKey,
    private val navigationRouter: NavigationRouter,
    private val offrampRepo: OfframpRepository,
    private val getOrderFeeDetails: GetOrderFeeDetailsUseCase,
) : ViewModel() {
    private val request: OfframpRequest = OfframpRequest(
        recipientUpi = args.recipientUpi,
        usdcAmount = Usdc6(BigInteger(args.usdcAmountMicro)),
        currency = args.currency,
    )

    private val persister = OfframpCheckpointPersister(repo = offrampRepo, request = request)

    private val feeDetails = MutableStateFlow<OrderFeeDetails?>(null)

    /**
     * Shared so multiple downstream collectors (state-builder, persister side effect, fee-details
     * fetcher) read a single underlying orchestrator run. `replay = 1` so late subscribers (the
     * fee-details fetcher launches from `init {}`) see the current status immediately.
     */
    private val statusSource: SharedFlow<OfframpStatus> = flow {
        val existing = offrampRepo.getInFlight()
        val upstream = if (existing != null && existing.orderIdBig != null) {
            Twig.info { "UpiOfframpProgress resuming order ${existing.orderId} from ${existing.currentStep}" }
            persister.seedFrom(existing)
            orchestrator.resume(existing)
        } else {
            if (existing != null) {
                Twig.warn { "UpiOfframpProgress: discarding pre-orderId checkpoint at ${existing.currentStep}" }
                offrampRepo.clear()
            }
            orchestrator.run(request)
        }
        upstream
            .onEach { status ->
                Twig.info { "UpiOfframpProgress status=$status" }
                persister.onStatus(status)
            }
            .collect { emit(it) }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        // Fee details: refetch whenever orderId or status-class changes. Distinct-until-changed
        // throttles the WaitingForCompletion poll loop (which emits every 3s) down to one fetch
        // per genuine state transition.
        viewModelScope.launch {
            statusSource
                .mapNotNull { status -> status.orderId?.let { it to status::class } }
                .distinctUntilChanged()
                .collect { (orderId, _) ->
                    getOrderFeeDetails(orderId)?.let { details -> feeDetails.update { details } }
                }
        }
    }

    val state: StateFlow<UpiOfframpProgressState> =
        combine(statusSource, feeDetails) { status, fees -> buildState(status, fees) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = buildState(OfframpStatus.Idle, null),
            )

    private fun buildState(status: OfframpStatus, fees: OrderFeeDetails?): UpiOfframpProgressState {
        val orderId = status.orderId
        val summary = buildSummary(status, orderId)

        val title = when (status) {
            is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_title_completed)
            is OfframpStatus.Cancelled -> stringRes(R.string.upi_offramp_progress_title_cancelled)
            is OfframpStatus.Failed -> stringRes(R.string.upi_offramp_progress_title_failed)
            else -> stringRes(R.string.upi_offramp_progress_title_in_progress)
        }

        val subtitle: StringResource? = when (status) {
            is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_subtitle_completed)
            is OfframpStatus.Cancelled -> stringRes(R.string.upi_offramp_progress_subtitle_cancelled)
            is OfframpStatus.Failed -> null
            else -> stringRes(R.string.upi_offramp_progress_subtitle_recipient, args.recipientUpi)
        }

        val steps = buildSteps(status)
        val failure = (status as? OfframpStatus.Failed)?.let(::buildFailureCard)
        val cancelled = (status as? OfframpStatus.Cancelled)?.let(::buildCancelledCard)
        val feeBreakdown = buildFeeBreakdown(fees)

        val primary = when (status) {
            is OfframpStatus.Completed -> ButtonState(
                text = stringRes(R.string.upi_offramp_progress_done_button),
                onClick = { navigationRouter.back() },
            )
            is OfframpStatus.Cancelled, is OfframpStatus.Failed -> ButtonState(
                text = stringRes(R.string.upi_offramp_progress_close_button),
                onClick = { navigationRouter.back() },
            )
            else -> null
        }

        return UpiOfframpProgressState(
            title = title,
            subtitle = subtitle,
            summary = summary,
            feeBreakdown = feeBreakdown,
            steps = steps,
            failure = failure,
            cancelled = cancelled,
            primaryButton = primary,
            onBack = { navigationRouter.back() },
        )
    }

    private fun buildSummary(status: OfframpStatus, orderId: BigInteger?): UpiOfframpOrderSummary {
        val completionDuration = (status as? OfframpStatus.Completed)?.let {
            completionDurationString(it.placedAtEpochSeconds, it.completedAtEpochSeconds)
        }
        val terminalTimestamp = when (status) {
            is OfframpStatus.Completed -> status.completedAtEpochSeconds?.let(::formatTerminalTimestamp)
            is OfframpStatus.Cancelled -> status.cancelledAtEpochSeconds?.let(::formatTerminalTimestamp)
            else -> null
        }
        val merchantAddress = when (status) {
            is OfframpStatus.Completed -> status.acceptedMerchant.checksumHex
            is OfframpStatus.Cancelled -> status.acceptedMerchant?.checksumHex
            else -> null
        }
        return UpiOfframpOrderSummary(
            amountUsdcDisplay = stringRes(
                R.string.upi_offramp_progress_amount_usdc,
                formatUsdc(args.usdcAmountMicro),
            ),
            recipient = args.recipientUpi,
            orderId = orderId?.toString(),
            networkName = network.name.replaceFirstChar { it.uppercase(Locale.ROOT) },
            signerAddress = account.address.checksumHex,
            signerExplorerUrl = explorerUrl(addressPath(account.address)),
            completionDuration = completionDuration,
            terminalTimestamp = terminalTimestamp,
            merchantAddress = merchantAddress,
            merchantExplorerUrl = merchantAddress?.let { explorerUrl("/address/$it") },
        )
    }

    private fun buildFeeBreakdown(fees: OrderFeeDetails?): UpiOfframpFeeBreakdown? {
        if (fees == null) return null
        // Pre-acceptance the contract returns all-zeros; rendering that gives a misleading
        // "fee = 0.00 USDC" line. Only surface the card once at least one value is meaningful.
        if (fees.fixedFeePaid == Usdc6.ZERO && fees.actualUsdcAmount == Usdc6.ZERO) return null
        val youSend = fees.actualUsdcAmount.takeIf { it > Usdc6.ZERO }
            ?.let { stringRes(R.string.upi_offramp_fee_breakdown_amount_usdc, formatUsdcDisplay(it)) }
        val fee = fees.fixedFeePaid.takeIf { it > Usdc6.ZERO }
            ?.let { stringRes(R.string.upi_offramp_fee_breakdown_amount_usdc, formatUsdcDisplay(it)) }
        val youReceive = fees.actualFiatAmount.takeIf { it > Usdc6.ZERO }
            ?.let { stringRes(R.string.upi_offramp_fee_breakdown_amount_inr, formatUsdcDisplay(it)) }
        return UpiOfframpFeeBreakdown(youSend = youSend, fee = fee, youReceive = youReceive)
    }

    private fun buildCancelledCard(cancelled: OfframpStatus.Cancelled): UpiOfframpCancelledCard {
        val refundedAmount = cancelled.refundedUsdcAmount?.let {
            stringRes(R.string.upi_offramp_cancelled_refunded, formatUsdcDisplay(it))
        }
        val cancelledAt = cancelled.cancelledAtEpochSeconds?.let {
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
            stepLabel = stepLabel(failed.step),
            decodedReason = decodedReason(failed),
            rawSelector = failed.revertSelector?.hex,
            rawMessage = rawForUi,
            txHash = txHashHex,
            txExplorerUrl = txHashHex?.let { explorerUrl(txPath(it)) },
        )
    }

    private fun decodedReason(failed: OfframpStatus.Failed): StringResource? {
        failed.knownRevertReason?.let { return stringRes(curatedRevertStringRes(it)) }
        failed.sdkErrorName?.let {
            return stringRes(R.string.upi_offramp_revert_sdk_long_tail, it)
        }
        return failed.solidityErrorString?.let(::stringRes)
    }

    private fun curatedRevertStringRes(reason: KnownRevertReason): Int = when (reason) {
        KnownRevertReason.BuyOrderAmountExceedsLimit -> R.string.upi_offramp_revert_buy_order_amount_exceeds_limit
        KnownRevertReason.InsufficientReputation -> R.string.upi_offramp_revert_insufficient_reputation
        KnownRevertReason.ZkVerificationRequired -> R.string.upi_offramp_revert_zk_verification_required
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
    }

    private fun buildSteps(status: OfframpStatus): List<UpiOfframpStep> {
        val order = OfframpStep.UI_PROGRESS
        val currentStep = status.step.takeIf { status !is OfframpStatus.Failed }
        val failedStep = (status as? OfframpStatus.Failed)?.step

        return order.mapIndexed { index, step ->
            val stepStatus = computeStepStatus(index, step, order, currentStep, failedStep, status)
            val txHashHex = txHashFor(status, step)?.hex
            UpiOfframpStep(
                label = stringRes(stepLabelRes(step)),
                status = stepStatus,
                txHash = txHashHex,
                txExplorerUrl = txHashHex?.let { explorerUrl(txPath(it)) },
                detailLines = stepDetail(status, step),
            )
        }
    }

    private fun computeStepStatus(
        index: Int,
        step: OfframpStep,
        order: List<OfframpStep>,
        currentStep: OfframpStep?,
        failedStep: OfframpStep?,
        status: OfframpStatus,
    ): UpiOfframpStepStatus {
        if (failedStep != null) {
            val failedAt = uiIndexFor(failedStep, order)
            return when {
                index == failedAt -> UpiOfframpStepStatus.Failed
                index < failedAt -> UpiOfframpStepStatus.Completed
                else -> UpiOfframpStepStatus.Pending
            }
        }
        // Cancelled is terminal: the WAITING_FOR_COMPLETION row didn't complete (paint Failed);
        // everything before it did happen on-chain (paint Completed).
        if (status is OfframpStatus.Cancelled) {
            val cancelledAt = uiIndexFor(OfframpStep.WAITING_FOR_COMPLETION, order)
            return when {
                index == cancelledAt -> UpiOfframpStepStatus.Failed
                index < cancelledAt -> UpiOfframpStepStatus.Completed
                else -> UpiOfframpStepStatus.Pending
            }
        }
        if (currentStep == null) return UpiOfframpStepStatus.Pending
        val currentIndex = uiIndexFor(currentStep, order)
        return when {
            index < currentIndex -> UpiOfframpStepStatus.Completed
            index == currentIndex -> UpiOfframpStepStatus.InProgress
            else -> UpiOfframpStepStatus.Pending
        }
    }

    /**
     * Maps a canonical [OfframpStep] to its position in [order]. INITIALIZATION collapses to
     * SELECTING_CIRCLE; ENCRYPTING_UPI collapses to SENDING_UPI (these are internal steps not
     * surfaced as separate UI rows).
     */
    private fun uiIndexFor(step: OfframpStep, order: List<OfframpStep>): Int {
        val displayed = when (step) {
            OfframpStep.INITIALIZATION -> OfframpStep.SELECTING_CIRCLE
            OfframpStep.ENCRYPTING_UPI -> OfframpStep.SENDING_UPI
            else -> step
        }
        return order.indexOf(displayed).coerceAtLeast(0)
    }

    private fun stepLabel(step: OfframpStep): StringResource = stringRes(stepLabelRes(step))

    private fun stepLabelRes(step: OfframpStep): Int = when (step) {
        OfframpStep.INITIALIZATION -> R.string.upi_offramp_step_init
        OfframpStep.SELECTING_CIRCLE -> R.string.upi_offramp_step_selecting_circle
        OfframpStep.APPROVING_USDC -> R.string.upi_offramp_step_approve
        OfframpStep.PLACING_ORDER -> R.string.upi_offramp_step_place_order
        OfframpStep.WAITING_FOR_ACCEPTANCE -> R.string.upi_offramp_step_wait_acceptance
        OfframpStep.ENCRYPTING_UPI -> R.string.upi_offramp_step_encrypting_upi
        OfframpStep.SENDING_UPI -> R.string.upi_offramp_step_send_upi
        OfframpStep.WAITING_FOR_COMPLETION -> R.string.upi_offramp_step_wait_completion
    }

    private fun txHashFor(status: OfframpStatus, step: OfframpStep): TxHash? = when (step) {
        OfframpStep.APPROVING_USDC -> (status as? OfframpStatus.ApprovingUsdc)?.txHash
        OfframpStep.PLACING_ORDER -> (status as? OfframpStatus.PlacingOrder)?.txHash
        OfframpStep.SENDING_UPI -> (status as? OfframpStatus.SendingEncryptedUpi)?.txHash
        else -> null
    }

    private fun stepDetail(status: OfframpStatus, step: OfframpStep): List<StringResource> = when (step) {
        OfframpStep.SELECTING_CIRCLE -> (status as? OfframpStatus.SelectingCircle)?.let {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_candidates, it.candidateCount))
                it.selectedCircleId?.let { circle ->
                    add(stringRes(R.string.upi_offramp_detail_selected_circle, circle.toString()))
                }
            }
        }.orEmpty()
        OfframpStep.APPROVING_USDC -> (status as? OfframpStatus.ApprovingUsdc)?.let {
            listOf(stringRes(R.string.upi_offramp_detail_amount, formatUsdc(it.amount.micros.toString())))
        }.orEmpty()
        OfframpStep.PLACING_ORDER -> (status as? OfframpStatus.PlacingOrder)?.let {
            listOf(
                stringRes(R.string.upi_offramp_detail_circle_id, it.circleId.toString()),
                stringRes(R.string.upi_offramp_detail_amount, formatUsdc(it.amount.micros.toString())),
            )
        }.orEmpty()
        OfframpStep.WAITING_FOR_ACCEPTANCE -> buildAcceptanceDetails(status)
        OfframpStep.SENDING_UPI -> (status as? OfframpStatus.SendingEncryptedUpi)?.let {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_merchant, it.merchantAddress.checksumHex))
                it.acceptedAtEpochSeconds?.let { ts ->
                    add(stringRes(R.string.upi_offramp_detail_accepted_at, formatTimestampValue(ts)))
                }
            }
        }.orEmpty()
        OfframpStep.WAITING_FOR_COMPLETION -> buildCompletionDetails(status)
        else -> emptyList()
    }

    private fun buildAcceptanceDetails(status: OfframpStatus): List<StringResource> =
        (status as? OfframpStatus.WaitingForMerchantAcceptance)?.let {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_polling_attempts, it.pollAttempts))
                it.lastObservedStatus?.let { last ->
                    add(stringRes(R.string.upi_offramp_detail_last_status, last.name))
                }
                if (it.stalled) add(stringRes(R.string.upi_offramp_detail_stalled))
            }
        }.orEmpty()

    private fun buildCompletionDetails(status: OfframpStatus): List<StringResource> = when (status) {
        is OfframpStatus.WaitingForCompletion -> buildList {
            add(stringRes(R.string.upi_offramp_detail_polling_attempts, status.pollAttempts))
            status.lastObservedStatus?.let { last ->
                add(stringRes(R.string.upi_offramp_detail_last_status, last.name))
            }
            status.acceptedAtEpochSeconds?.let { ts ->
                add(stringRes(R.string.upi_offramp_detail_accepted_at, formatTimestampValue(ts)))
            }
            status.paidAtEpochSeconds?.let { ts ->
                add(stringRes(R.string.upi_offramp_detail_paid_at, formatTimestampValue(ts)))
            }
            if (status.stalled) add(stringRes(R.string.upi_offramp_detail_stalled))
        }
        is OfframpStatus.Completed -> buildList {
            add(stringRes(R.string.upi_offramp_detail_merchant, status.acceptedMerchant.checksumHex))
            status.paidAtEpochSeconds?.let { ts ->
                add(stringRes(R.string.upi_offramp_detail_paid_at, formatTimestampValue(ts)))
            }
            status.completedAtEpochSeconds?.let { ts ->
                add(stringRes(R.string.upi_offramp_detail_completed_at, formatTimestampValue(ts)))
            }
        }
        else -> emptyList()
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

    private fun formatTimestampValue(epochSeconds: Long): String =
        clockFormat.format(Date(epochSeconds * MILLIS_PER_SECOND))

    private fun explorerUrl(path: String): String =
        network.baseExplorerUrl.trimEnd('/') + path

    private fun addressPath(address: Address): String = "/address/${address.checksumHex}"
    private fun txPath(hash: String): String = "/tx/$hash"

    private fun formatUsdc(microString: String): String {
        val micros = runCatching { BigInteger(microString) }.getOrElse { return "0" }
        return BigDecimal(micros).movePointLeft(USDC_DECIMALS).toPlainString()
    }

    private fun formatUsdcDisplay(value: Usdc6): String = formatUsdc(value.micros.toString())

    companion object {
        private const val USDC_DECIMALS = 6
        private const val MAX_RAW_MESSAGE_LEN = 500
        private const val SECONDS_PER_MINUTE = 60L
        private const val MILLIS_PER_SECOND = 1_000L

        // Locale-stable formats so screenshots / fixtures don't drift across devices.
        private val terminalDateFormat: SimpleDateFormat =
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
        private val clockFormat: SimpleDateFormat =
            SimpleDateFormat("HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
    }
}

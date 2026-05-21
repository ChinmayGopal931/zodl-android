package co.electriccoin.zcash.ui.screen.swap.upi.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.orchestrator.FailedStep
import xyz.justzappit.offramp.orchestrator.OfframpOrchestrator
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("TooManyFunctions")
internal class UpiOfframpProgressVM(
    private val args: UpiOfframpProgressArgs,
    private val orchestrator: OfframpOrchestrator,
    private val network: P2pNetworkConfig,
    private val account: EvmKey,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val latestStatus = MutableStateFlow<OfframpStatus>(OfframpStatus.Idle)

    val state: StateFlow<UpiOfframpProgressState> = latestStatus
        .map(::buildState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = buildState(OfframpStatus.Idle),
        )

    init {
        viewModelScope.launch { runOrder() }
    }

    private suspend fun runOrder() {
        val request = OfframpRequest(
            recipientUpi = args.recipientUpi,
            usdcAmount = BigInteger(args.usdcAmountMicro),
            currency = args.currency,
        )
        orchestrator
            .run(request)
            .onEach { status ->
                Twig.info { "UpiOfframpProgress status=$status" }
                latestStatus.update { status }
            }
            .collect { /* state already updated in onEach */ }
    }

    private fun buildState(status: OfframpStatus): UpiOfframpProgressState {
        val orderId = extractOrderId(status)
        val summary = UpiOfframpOrderSummary(
            amountUsdcDisplay = stringRes(
                R.string.upi_offramp_progress_amount_usdc,
                formatUsdc(args.usdcAmountMicro),
            ),
            recipient = args.recipientUpi,
            orderId = orderId?.toString(),
            networkName = network.name.replaceFirstChar { it.uppercase() },
            signerAddress = account.address,
            signerExplorerUrl = explorerUrl(addressPath(account.address)),
        )

        val title = when (status) {
            is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_title_completed)
            is OfframpStatus.Failed -> stringRes(R.string.upi_offramp_progress_title_failed)
            else -> stringRes(R.string.upi_offramp_progress_title_in_progress)
        }

        val subtitle: StringResource? = when (status) {
            is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_subtitle_completed)
            is OfframpStatus.Failed -> null
            else -> stringRes(R.string.upi_offramp_progress_subtitle_recipient, args.recipientUpi)
        }

        val steps = buildSteps(status)
        val failure = (status as? OfframpStatus.Failed)?.let(::buildFailureCard)

        val primary = when (status) {
            is OfframpStatus.Completed -> ButtonState(
                text = stringRes(R.string.upi_offramp_progress_done_button),
                onClick = { navigationRouter.back() },
            )
            is OfframpStatus.Failed -> ButtonState(
                text = stringRes(R.string.upi_offramp_progress_close_button),
                onClick = { navigationRouter.back() },
            )
            else -> null
        }

        return UpiOfframpProgressState(
            title = title,
            subtitle = subtitle,
            summary = summary,
            steps = steps,
            failure = failure,
            primaryButton = primary,
            onBack = { navigationRouter.back() },
        )
    }

    private fun extractOrderId(status: OfframpStatus): BigInteger? = when (status) {
        is OfframpStatus.WaitingForMerchantAcceptance -> status.orderId
        is OfframpStatus.SendingEncryptedUpi -> status.orderId
        is OfframpStatus.WaitingForCompletion -> status.orderId
        is OfframpStatus.Completed -> status.orderId
        is OfframpStatus.Failed -> status.orderId
        else -> null
    }

    private fun buildFailureCard(failed: OfframpStatus.Failed): UpiOfframpFailureCard {
        val rawForUi = failed.message.take(MAX_RAW_MESSAGE_LEN)
        return UpiOfframpFailureCard(
            stepLabel = stepLabel(failed.step),
            decodedReason = failed.decodedReason?.let(::stringRes),
            rawSelector = failed.revertSelector,
            rawMessage = rawForUi,
            txHash = failed.txHash,
            txExplorerUrl = failed.txHash?.let { explorerUrl(txPath(it)) },
        )
    }

    private fun buildSteps(status: OfframpStatus): List<UpiOfframpStep> {
        val order = listOf(
            STEP_SELECTING_CIRCLE,
            STEP_APPROVE,
            STEP_PLACE_ORDER,
            STEP_WAIT_ACCEPTANCE,
            STEP_SEND_UPI,
            STEP_WAIT_COMPLETION,
        )
        val currentIndex = currentStepIndex(status)
        val failedIndex = (status as? OfframpStatus.Failed)?.let { failedStepIndex(it.step) }

        return order.mapIndexed { index, key ->
            val stepStatus = when {
                failedIndex != null && index == failedIndex -> UpiOfframpStepStatus.Failed
                failedIndex != null && index < failedIndex -> UpiOfframpStepStatus.Completed
                failedIndex != null -> UpiOfframpStepStatus.Pending
                index < currentIndex -> UpiOfframpStepStatus.Completed
                index == currentIndex -> UpiOfframpStepStatus.InProgress
                else -> UpiOfframpStepStatus.Pending
            }
            val txHash = txHashFor(status, key)
            UpiOfframpStep(
                label = stringRes(stepLabelRes(key)),
                status = stepStatus,
                txHash = txHash,
                txExplorerUrl = txHash?.let { explorerUrl(txPath(it)) },
                detailLines = stepDetail(status, key),
            )
        }
    }

    private fun currentStepIndex(status: OfframpStatus): Int = when (status) {
        is OfframpStatus.Idle -> -1
        is OfframpStatus.SelectingCircle -> 0
        is OfframpStatus.ApprovingUsdc -> 1
        is OfframpStatus.PlacingOrder -> 2
        is OfframpStatus.WaitingForMerchantAcceptance -> 3
        is OfframpStatus.SendingEncryptedUpi -> 4
        is OfframpStatus.WaitingForCompletion -> 5
        is OfframpStatus.Completed -> 6
        is OfframpStatus.Failed -> -1
    }

    private fun failedStepIndex(step: FailedStep): Int = when (step) {
        FailedStep.INITIALIZATION, FailedStep.SELECTING_CIRCLE -> 0
        FailedStep.APPROVING_USDC -> 1
        FailedStep.PLACING_ORDER -> 2
        FailedStep.WAITING_FOR_ACCEPTANCE -> 3
        FailedStep.ENCRYPTING_UPI, FailedStep.SENDING_UPI -> 4
        FailedStep.WAITING_FOR_COMPLETION -> 5
    }

    private fun stepLabel(step: FailedStep): StringResource = stringRes(
        when (step) {
            FailedStep.INITIALIZATION -> R.string.upi_offramp_step_init
            FailedStep.SELECTING_CIRCLE -> R.string.upi_offramp_step_selecting_circle
            FailedStep.APPROVING_USDC -> R.string.upi_offramp_step_approve
            FailedStep.PLACING_ORDER -> R.string.upi_offramp_step_place_order
            FailedStep.WAITING_FOR_ACCEPTANCE -> R.string.upi_offramp_step_wait_acceptance
            FailedStep.ENCRYPTING_UPI -> R.string.upi_offramp_step_encrypting_upi
            FailedStep.SENDING_UPI -> R.string.upi_offramp_step_send_upi
            FailedStep.WAITING_FOR_COMPLETION -> R.string.upi_offramp_step_wait_completion
        },
    )

    private fun stepLabelRes(key: String): Int = when (key) {
        STEP_SELECTING_CIRCLE -> R.string.upi_offramp_step_selecting_circle
        STEP_APPROVE -> R.string.upi_offramp_step_approve
        STEP_PLACE_ORDER -> R.string.upi_offramp_step_place_order
        STEP_WAIT_ACCEPTANCE -> R.string.upi_offramp_step_wait_acceptance
        STEP_SEND_UPI -> R.string.upi_offramp_step_send_upi
        STEP_WAIT_COMPLETION -> R.string.upi_offramp_step_wait_completion
        else -> R.string.upi_offramp_step_selecting_circle
    }

    private fun txHashFor(status: OfframpStatus, key: String): String? = when {
        key == STEP_APPROVE && status is OfframpStatus.ApprovingUsdc -> status.txHash
        key == STEP_PLACE_ORDER && status is OfframpStatus.PlacingOrder -> status.txHash
        key == STEP_SEND_UPI && status is OfframpStatus.SendingEncryptedUpi -> status.txHash
        else -> null
    }

    private fun stepDetail(status: OfframpStatus, key: String): List<StringResource> = when {
        key == STEP_SELECTING_CIRCLE && status is OfframpStatus.SelectingCircle -> buildList {
            add(stringRes(R.string.upi_offramp_detail_candidates, status.candidateCount))
            status.selectedCircleId?.let {
                add(stringRes(R.string.upi_offramp_detail_selected_circle, it.toString()))
            }
        }
        key == STEP_APPROVE && status is OfframpStatus.ApprovingUsdc ->
            listOf(stringRes(R.string.upi_offramp_detail_amount, formatUsdc(status.amount.toString())))
        key == STEP_PLACE_ORDER && status is OfframpStatus.PlacingOrder -> listOf(
            stringRes(R.string.upi_offramp_detail_circle_id, status.circleId.toString()),
            stringRes(R.string.upi_offramp_detail_amount, formatUsdc(status.amount.toString())),
        )
        key == STEP_WAIT_ACCEPTANCE && status is OfframpStatus.WaitingForMerchantAcceptance -> buildList {
            add(stringRes(R.string.upi_offramp_detail_polling_attempts, status.pollAttempts))
            status.lastObservedStatus?.let {
                add(stringRes(R.string.upi_offramp_detail_last_status, it.name))
            }
        }
        key == STEP_SEND_UPI && status is OfframpStatus.SendingEncryptedUpi -> listOf(
            stringRes(R.string.upi_offramp_detail_merchant, status.merchantAddress),
        )
        key == STEP_WAIT_COMPLETION && status is OfframpStatus.WaitingForCompletion -> buildList {
            add(stringRes(R.string.upi_offramp_detail_polling_attempts, status.pollAttempts))
            status.lastObservedStatus?.let {
                add(stringRes(R.string.upi_offramp_detail_last_status, it.name))
            }
        }
        key == STEP_WAIT_COMPLETION && status is OfframpStatus.Completed ->
            listOf(stringRes(R.string.upi_offramp_detail_merchant, status.acceptedMerchant))
        else -> emptyList()
    }

    private fun explorerUrl(path: String): String =
        network.baseExplorerUrl.trimEnd('/') + path

    private fun addressPath(address: String): String = "/address/$address"
    private fun txPath(hash: String): String = "/tx/$hash"

    private fun formatUsdc(microString: String): String {
        val micros = runCatching { BigInteger(microString) }.getOrElse { return "0" }
        return BigDecimal(micros).movePointLeft(USDC_DECIMALS).toPlainString()
    }

    companion object {
        private const val STEP_SELECTING_CIRCLE = "selecting_circle"
        private const val STEP_APPROVE = "approve"
        private const val STEP_PLACE_ORDER = "place_order"
        private const val STEP_WAIT_ACCEPTANCE = "wait_acceptance"
        private const val STEP_SEND_UPI = "send_upi"
        private const val STEP_WAIT_COMPLETION = "wait_completion"
        private const val USDC_DECIMALS = 6
        private const val MAX_RAW_MESSAGE_LEN = 500
    }
}

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
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.orchestrator.KnownRevertReason
import xyz.justzappit.offramp.orchestrator.OfframpOrchestrator
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import xyz.justzappit.offramp.orchestrator.OfframpStep
import xyz.justzappit.offramp.orchestrator.step
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
            signerAddress = account.address.checksumHex,
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
            decodedReason = decodedReason(failed),
            rawSelector = failed.revertSelector?.hex,
            rawMessage = rawForUi,
            txHash = failed.txHash,
            txExplorerUrl = failed.txHash?.let { explorerUrl(txPath(it)) },
        )
    }

    private fun decodedReason(failed: OfframpStatus.Failed): StringResource? = when (val reason = failed.knownRevertReason) {
        KnownRevertReason.InsufficientReputation -> stringRes(R.string.upi_offramp_revert_insufficient_reputation)
        KnownRevertReason.NoMerchantLiquidity -> stringRes(R.string.upi_offramp_revert_no_merchant_liquidity)
        null -> failed.solidityErrorString?.let(::stringRes)
    }

    private fun buildSteps(status: OfframpStatus): List<UpiOfframpStep> {
        val order = OfframpStep.UI_PROGRESS
        val currentStep = status.step.takeIf { status !is OfframpStatus.Failed }
        val failedStep = (status as? OfframpStatus.Failed)?.step

        return order.mapIndexed { index, step ->
            val stepStatus = computeStepStatus(index, step, order, currentStep, failedStep)
            val txHash = txHashFor(status, step)
            UpiOfframpStep(
                label = stringRes(stepLabelRes(step)),
                status = stepStatus,
                txHash = txHash,
                txExplorerUrl = txHash?.let { explorerUrl(txPath(it)) },
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
    ): UpiOfframpStepStatus {
        if (failedStep != null) {
            val failedAt = uiIndexFor(failedStep, order)
            return when {
                index == failedAt -> UpiOfframpStepStatus.Failed
                index < failedAt -> UpiOfframpStepStatus.Completed
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

    private fun txHashFor(status: OfframpStatus, step: OfframpStep): String? = when (step) {
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
            listOf(stringRes(R.string.upi_offramp_detail_amount, formatUsdc(it.amount.toString())))
        }.orEmpty()
        OfframpStep.PLACING_ORDER -> (status as? OfframpStatus.PlacingOrder)?.let {
            listOf(
                stringRes(R.string.upi_offramp_detail_circle_id, it.circleId.toString()),
                stringRes(R.string.upi_offramp_detail_amount, formatUsdc(it.amount.toString())),
            )
        }.orEmpty()
        OfframpStep.WAITING_FOR_ACCEPTANCE -> (status as? OfframpStatus.WaitingForMerchantAcceptance)?.let {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_polling_attempts, it.pollAttempts))
                it.lastObservedStatus?.let { last ->
                    add(stringRes(R.string.upi_offramp_detail_last_status, last.name))
                }
            }
        }.orEmpty()
        OfframpStep.SENDING_UPI -> (status as? OfframpStatus.SendingEncryptedUpi)?.let {
            listOf(stringRes(R.string.upi_offramp_detail_merchant, it.merchantAddress.checksumHex))
        }.orEmpty()
        OfframpStep.WAITING_FOR_COMPLETION -> when (status) {
            is OfframpStatus.WaitingForCompletion -> buildList {
                add(stringRes(R.string.upi_offramp_detail_polling_attempts, status.pollAttempts))
                status.lastObservedStatus?.let { last ->
                    add(stringRes(R.string.upi_offramp_detail_last_status, last.name))
                }
            }
            is OfframpStatus.Completed -> listOf(
                stringRes(R.string.upi_offramp_detail_merchant, status.acceptedMerchant.checksumHex),
            )
            else -> emptyList()
        }
        else -> emptyList()
    }

    private fun explorerUrl(path: String): String =
        network.baseExplorerUrl.trimEnd('/') + path

    private fun addressPath(address: Address): String = "/address/${address.checksumHex}"
    private fun txPath(hash: String): String = "/tx/$hash"

    private fun formatUsdc(microString: String): String {
        val micros = runCatching { BigInteger(microString) }.getOrElse { return "0" }
        return BigDecimal(micros).movePointLeft(USDC_DECIMALS).toPlainString()
    }

    companion object {
        private const val USDC_DECIMALS = 6
        private const val MAX_RAW_MESSAGE_LEN = 500
    }
}

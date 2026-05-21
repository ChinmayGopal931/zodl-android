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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.justzappit.offramp.orchestrator.OfframpOrchestrator
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import java.math.BigInteger

internal class UpiOfframpProgressVM(
    private val args: UpiOfframpProgressArgs,
    private val orchestrator: OfframpOrchestrator,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val latestStatus = MutableStateFlow<OfframpStatus>(OfframpStatus.Idle)

    val state: StateFlow<UpiOfframpProgressState> =
        latestStatus
            .let { source ->
                kotlinx.coroutines.flow.combine(source, MutableStateFlow(Unit)) { status, _ ->
                    buildState(status)
                }
            }
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
                latestStatus.value = status
            }
            .catch { cause ->
                Twig.warn(cause) { "UpiOfframpProgress orchestrator threw" }
                latestStatus.value = OfframpStatus.Failed(
                    message = cause.message ?: cause::class.simpleName ?: "Unknown error",
                    orderId = null,
                    cause = cause,
                )
            }
            .collect { /* state already updated in onEach */ }
    }

    private fun buildState(status: OfframpStatus): UpiOfframpProgressState {
        val steps = mapStatusToSteps(status)
        val title = when (status) {
            is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_title_completed)
            is OfframpStatus.Failed -> stringRes(R.string.upi_offramp_progress_title_failed)
            else -> stringRes(R.string.upi_offramp_progress_title_in_progress)
        }
        val subtitle: StringResource? = when (status) {
            is OfframpStatus.Failed -> stringRes(R.string.upi_offramp_progress_subtitle_failed, status.message)
            is OfframpStatus.Completed -> stringRes(R.string.upi_offramp_progress_subtitle_completed)
            else -> stringRes(R.string.upi_offramp_progress_subtitle_recipient, args.recipientUpi)
        }
        val (primary, secondary) = when (status) {
            is OfframpStatus.Completed -> ButtonState(
                text = stringRes(R.string.upi_offramp_progress_done_button),
                onClick = { navigationRouter.back() },
            ) to null
            is OfframpStatus.Failed -> ButtonState(
                text = stringRes(R.string.upi_offramp_progress_close_button),
                onClick = { navigationRouter.back() },
            ) to null
            else -> null to null
        }
        return UpiOfframpProgressState(
            title = title,
            subtitle = subtitle,
            steps = steps,
            primaryButton = primary,
            secondaryButton = secondary,
            onBack = { navigationRouter.back() },
        )
    }

    private fun mapStatusToSteps(status: OfframpStatus): List<UpiOfframpStep> {
        val order = listOf(
            STEP_SELECTING_CIRCLE,
            STEP_APPROVE,
            STEP_PLACE_ORDER,
            STEP_WAIT_ACCEPTANCE,
            STEP_SEND_UPI,
            STEP_WAIT_COMPLETION,
        )
        val currentIndex = when (status) {
            is OfframpStatus.Idle -> -1
            is OfframpStatus.SelectingCircle -> 0
            is OfframpStatus.ApprovingUsdc -> 1
            is OfframpStatus.PlacingOrder -> 2
            is OfframpStatus.WaitingForMerchantAcceptance -> 3
            is OfframpStatus.SendingEncryptedUpi -> 4
            is OfframpStatus.WaitingForCompletion -> 5
            is OfframpStatus.Completed -> order.size
            is OfframpStatus.Failed -> -1
        }
        return order.mapIndexed { index, key ->
            val stepStatus = when {
                status is OfframpStatus.Failed && index == lastActiveStepIndex(status) ->
                    UpiOfframpStepStatus.Failed
                index < currentIndex -> UpiOfframpStepStatus.Completed
                index == currentIndex -> UpiOfframpStepStatus.InProgress
                else -> UpiOfframpStepStatus.Pending
            }
            UpiOfframpStep(
                label = stringRes(stepLabelRes(key)),
                detail = stepDetail(status, key),
                status = stepStatus,
            )
        }
    }

    private fun lastActiveStepIndex(failed: OfframpStatus.Failed): Int =
        if (failed.orderId == null) 0 else 3

    private fun stepLabelRes(key: String): Int = when (key) {
        STEP_SELECTING_CIRCLE -> R.string.upi_offramp_step_selecting_circle
        STEP_APPROVE -> R.string.upi_offramp_step_approve
        STEP_PLACE_ORDER -> R.string.upi_offramp_step_place_order
        STEP_WAIT_ACCEPTANCE -> R.string.upi_offramp_step_wait_acceptance
        STEP_SEND_UPI -> R.string.upi_offramp_step_send_upi
        STEP_WAIT_COMPLETION -> R.string.upi_offramp_step_wait_completion
        else -> R.string.upi_offramp_step_selecting_circle
    }

    private fun stepDetail(status: OfframpStatus, key: String): StringResource? = when {
        key == STEP_APPROVE && status is OfframpStatus.ApprovingUsdc ->
            stringRes(R.string.upi_offramp_progress_tx_hash, status.txHash.take(TX_HASH_PREFIX_LEN))
        key == STEP_PLACE_ORDER && status is OfframpStatus.PlacingOrder ->
            stringRes(R.string.upi_offramp_progress_tx_hash, status.txHash.take(TX_HASH_PREFIX_LEN))
        key == STEP_SEND_UPI && status is OfframpStatus.SendingEncryptedUpi ->
            stringRes(R.string.upi_offramp_progress_tx_hash, status.txHash.take(TX_HASH_PREFIX_LEN))
        else -> null
    }

    companion object {
        private const val STEP_SELECTING_CIRCLE = "selecting_circle"
        private const val STEP_APPROVE = "approve"
        private const val STEP_PLACE_ORDER = "place_order"
        private const val STEP_WAIT_ACCEPTANCE = "wait_acceptance"
        private const val STEP_SEND_UPI = "send_upi"
        private const val STEP_WAIT_COMPLETION = "wait_completion"
        private const val TX_HASH_PREFIX_LEN = 12
    }
}

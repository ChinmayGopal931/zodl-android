package co.electriccoin.zcash.ui.screen.swap.upi.progress

import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.ellipsizeMiddle
import co.electriccoin.zcash.ui.design.util.stringRes
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.orchestrator.OfframpStatus
import xyz.justzappit.offramp.orchestrator.OfframpStep
import xyz.justzappit.offramp.orchestrator.step
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure mapper: orchestrator [OfframpStatus] → ordered list of [UpiOfframpStep] rows for the UI.
 *
 * Extracted from [UpiOfframpProgressVM] so it can be unit-tested without standing up a ViewModel
 * + Dispatchers.Main, and so the VM stays focused on flow plumbing. All inputs are values; the
 * only effect is producing the step list.
 */
internal fun buildProgressSteps(
    status: OfframpStatus,
    network: P2pNetworkConfig,
    fundedFromBaseObserved: Boolean = false,
): List<UpiOfframpStep> {
    val order = OfframpStep.UI_PROGRESS
    val currentStep = status.step.takeIf { status !is OfframpStatus.Failed }
    val failedStep = (status as? OfframpStatus.Failed)?.step
    return order.mapIndexed { index, step ->
        val txHashHex = txHashFor(status, step)?.hex
        UpiOfframpStep(
            label = stringRes(labelResFor(step, status, fundedFromBaseObserved)),
            status = computeStepStatus(index, order, currentStep, failedStep, status),
            txHash = txHashHex,
            txExplorerUrl = txHashHex?.let { network.txUrl(it) },
            detailLines = stepDetail(status, step),
        )
    }
}

/**
 * Picks the row label for [step]. Same row, different copy when the funding seam short-circuited —
 * we render "Using Base balance" instead of "Bridging funds". The VM tracks [fundedFromBaseObserved]
 * across the run (true once any [OfframpStatus.FundedFromBase] is seen) so the label sticks even
 * after the flow advances to later steps and the live status is no longer the funding one.
 */
private fun labelResFor(
    step: OfframpStep,
    status: OfframpStatus,
    fundedFromBaseObserved: Boolean,
): Int =
    when {
        step == OfframpStep.FUNDING && (status is OfframpStatus.FundedFromBase || fundedFromBaseObserved) ->
            R.string.upi_offramp_step_funding_from_base

        // The final row reads "Waiting for merchant payment" while polling; once COMPLETED it would
        // misleadingly still say "waiting", so flip it to a done label.
        step == OfframpStep.WAITING_FOR_COMPLETION && status is OfframpStatus.Completed ->
            R.string.upi_offramp_step_completed

        else -> stepLabelRes(step)
    }

private fun computeStepStatus(
    index: Int,
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
    // Completed is terminal success: every row is done, including the final completion row (which the
    // status->step mapping otherwise reports as the "current" step and would paint InProgress).
    if (status is OfframpStatus.Completed) return UpiOfframpStepStatus.Completed
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
    val displayed =
        when (step) {
            OfframpStep.INITIALIZATION -> OfframpStep.SELECTING_CIRCLE
            OfframpStep.ENCRYPTING_UPI -> OfframpStep.SENDING_UPI
            else -> step
        }
    return order.indexOf(displayed).coerceAtLeast(0)
}

internal fun stepLabelRes(step: OfframpStep): Int =
    when (step) {
        OfframpStep.INITIALIZATION -> R.string.upi_offramp_step_init
        OfframpStep.SELECTING_CIRCLE -> R.string.upi_offramp_step_selecting_circle
        OfframpStep.FUNDING -> R.string.upi_offramp_step_funding
        OfframpStep.APPROVING_USDC -> R.string.upi_offramp_step_approve
        OfframpStep.PLACING_ORDER -> R.string.upi_offramp_step_place_order
        OfframpStep.WAITING_FOR_ACCEPTANCE -> R.string.upi_offramp_step_wait_acceptance
        OfframpStep.ENCRYPTING_UPI -> R.string.upi_offramp_step_encrypting_upi
        OfframpStep.SENDING_UPI -> R.string.upi_offramp_step_send_upi
        OfframpStep.WAITING_FOR_COMPLETION -> R.string.upi_offramp_step_wait_completion
    }

private fun txHashFor(status: OfframpStatus, step: OfframpStep): TxHash? =
    when (step) {
        OfframpStep.APPROVING_USDC -> (status as? OfframpStatus.ApprovingUsdc)?.txHash
        OfframpStep.PLACING_ORDER -> (status as? OfframpStatus.PlacingOrder)?.txHash
        OfframpStep.SENDING_UPI -> (status as? OfframpStatus.SendingEncryptedUpi)?.txHash
        else -> null
    }

private fun stepDetail(status: OfframpStatus, step: OfframpStep): List<StringResource> =
    when (step) {
        OfframpStep.SELECTING_CIRCLE -> {
            (status as? OfframpStatus.SelectingCircle)
                ?.let {
                    buildList {
                        add(stringRes(R.string.upi_offramp_detail_candidates, it.candidateCount))
                        it.selectedCircleId?.let { circle ->
                            add(stringRes(R.string.upi_offramp_detail_selected_circle, circle.toString()))
                        }
                    }
                }.orEmpty()
        }

        OfframpStep.FUNDING -> {
            (status as? OfframpStatus.BridgingFunds)
                ?.let {
                    buildList {
                        add(stringRes(R.string.upi_offramp_detail_bridging_amount, it.amount.toDisplayString()))
                        it.depositAddress?.let { addr ->
                            add(
                                stringRes(
                                    R.string.upi_offramp_detail_deposit_addr,
                                    addr.ellipsizeMiddle(DEPOSIT_ELLIPSIS_PREFIX, DEPOSIT_ELLIPSIS_SUFFIX)
                                )
                            )
                        }
                    }
                }.orEmpty()
        }

        OfframpStep.APPROVING_USDC -> {
            (status as? OfframpStatus.ApprovingUsdc)
                ?.let {
                    listOf(stringRes(R.string.upi_offramp_detail_amount, it.amount.toDisplayString()))
                }.orEmpty()
        }

        OfframpStep.PLACING_ORDER -> {
            (status as? OfframpStatus.PlacingOrder)
                ?.let {
                    listOf(
                        stringRes(R.string.upi_offramp_detail_circle_id, it.circleId.toString()),
                        stringRes(R.string.upi_offramp_detail_amount, it.amount.toDisplayString()),
                    )
                }.orEmpty()
        }

        OfframpStep.WAITING_FOR_ACCEPTANCE -> {
            buildAcceptanceDetails(status)
        }

        OfframpStep.SENDING_UPI -> {
            (status as? OfframpStatus.SendingEncryptedUpi)
                ?.let {
                    buildList {
                        add(stringRes(R.string.upi_offramp_detail_merchant, it.merchantAddress.checksumHex))
                        it.acceptedAtEpochSeconds?.let { ts ->
                            add(stringRes(R.string.upi_offramp_detail_accepted_at, formatClockTime(ts)))
                        }
                    }
                }.orEmpty()
        }

        OfframpStep.WAITING_FOR_COMPLETION -> {
            buildCompletionDetails(status)
        }

        else -> {
            emptyList()
        }
    }

private fun buildAcceptanceDetails(status: OfframpStatus): List<StringResource> =
    (status as? OfframpStatus.WaitingForMerchantAcceptance)
        ?.let {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_polling_attempts, it.pollAttempts))
                it.lastObservedStatus?.let { last ->
                    add(stringRes(R.string.upi_offramp_detail_last_status, last.name))
                }
                if (it.stalled || it.expired) add(stringRes(R.string.upi_offramp_detail_stalled))
            }
        }.orEmpty()

private fun buildCompletionDetails(status: OfframpStatus): List<StringResource> =
    when (status) {
        is OfframpStatus.WaitingForCompletion -> {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_polling_attempts, status.pollAttempts))
                status.lastObservedStatus?.let { last ->
                    add(stringRes(R.string.upi_offramp_detail_last_status, last.name))
                }
                status.acceptedAtEpochSeconds?.let { ts ->
                    add(stringRes(R.string.upi_offramp_detail_accepted_at, formatClockTime(ts)))
                }
                status.paidAtEpochSeconds?.let { ts ->
                    add(stringRes(R.string.upi_offramp_detail_paid_at, formatClockTime(ts)))
                }
                if (status.stalled || status.expired) add(stringRes(R.string.upi_offramp_detail_stalled))
            }
        }

        is OfframpStatus.Completed -> {
            buildList {
                add(stringRes(R.string.upi_offramp_detail_merchant, status.acceptedMerchant.checksumHex))
                status.paidAtEpochSeconds?.let { ts ->
                    add(stringRes(R.string.upi_offramp_detail_paid_at, formatClockTime(ts)))
                }
                status.completedAtEpochSeconds?.let { ts ->
                    add(stringRes(R.string.upi_offramp_detail_completed_at, formatClockTime(ts)))
                }
            }
        }

        else -> {
            emptyList()
        }
    }

private fun formatClockTime(epochSeconds: Long): String =
    clockFormat.format(Date(epochSeconds * MILLIS_PER_SECOND))

// Locale-stable format so screenshots / fixtures don't drift across devices.
private val clockFormat: SimpleDateFormat =
    SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getDefault() }

private const val MILLIS_PER_SECOND = 1_000L
private const val DEPOSIT_ELLIPSIS_PREFIX = 10
private const val DEPOSIT_ELLIPSIS_SUFFIX = 6

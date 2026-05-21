package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.evm.abi.Selector4
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.p2p.OrderStatus
import java.math.BigInteger

sealed class OfframpStatus {
    object Idle : OfframpStatus()

    data class SelectingCircle(
        val candidateCount: Int,
        val selectedCircleId: BigInteger? = null,
    ) : OfframpStatus()

    data class ApprovingUsdc(
        val txHash: String,
        val amount: BigInteger,
    ) : OfframpStatus()

    data class PlacingOrder(
        val txHash: String,
        val circleId: BigInteger,
        val amount: BigInteger,
    ) : OfframpStatus()

    data class WaitingForMerchantAcceptance(
        val orderId: BigInteger,
        val pollAttempts: Int = 0,
        val lastObservedStatus: OrderStatus? = null,
    ) : OfframpStatus()

    data class SendingEncryptedUpi(
        val orderId: BigInteger,
        val txHash: String,
        val merchantAddress: Address,
        val merchantPubKey: String,
    ) : OfframpStatus()

    data class WaitingForCompletion(
        val orderId: BigInteger,
        val pollAttempts: Int = 0,
        val lastObservedStatus: OrderStatus? = null,
    ) : OfframpStatus()

    data class Completed(
        val orderId: BigInteger,
        val acceptedMerchant: Address,
        val actualUsdcAmount: BigInteger? = null,
        val actualFiatAmount: BigInteger? = null,
        val completedAtEpochSeconds: Long? = null,
    ) : OfframpStatus()

    data class Failed(
        val message: String,
        val orderId: BigInteger?,
        val step: OfframpStep,
        val txHash: String? = null,
        val revertSelector: Selector4? = null,
        val knownRevertReason: KnownRevertReason? = null,
        val solidityErrorString: String? = null,
        val cause: Throwable? = null,
    ) : OfframpStatus()
}

/**
 * Canonical state-machine step. Shared between orchestrator state tracking, [OfframpStatus.Failed.step],
 * and the UI progress indicator — no parallel encodings.
 */
enum class OfframpStep {
    INITIALIZATION,
    SELECTING_CIRCLE,
    APPROVING_USDC,
    PLACING_ORDER,
    WAITING_FOR_ACCEPTANCE,
    ENCRYPTING_UPI,
    SENDING_UPI,
    WAITING_FOR_COMPLETION,
    ;

    companion object {
        /** Steps surfaced to the UI progress indicator (skips INITIALIZATION + ENCRYPTING_UPI). */
        val UI_PROGRESS: List<OfframpStep> = listOf(
            SELECTING_CIRCLE,
            APPROVING_USDC,
            PLACING_ORDER,
            WAITING_FOR_ACCEPTANCE,
            SENDING_UPI,
            WAITING_FOR_COMPLETION,
        )
    }
}

/** Derives the canonical [OfframpStep] from any [OfframpStatus] instance. */
val OfframpStatus.step: OfframpStep get() = when (this) {
    OfframpStatus.Idle -> OfframpStep.INITIALIZATION
    is OfframpStatus.SelectingCircle -> OfframpStep.SELECTING_CIRCLE
    is OfframpStatus.ApprovingUsdc -> OfframpStep.APPROVING_USDC
    is OfframpStatus.PlacingOrder -> OfframpStep.PLACING_ORDER
    is OfframpStatus.WaitingForMerchantAcceptance -> OfframpStep.WAITING_FOR_ACCEPTANCE
    is OfframpStatus.SendingEncryptedUpi -> OfframpStep.SENDING_UPI
    is OfframpStatus.WaitingForCompletion -> OfframpStep.WAITING_FOR_COMPLETION
    is OfframpStatus.Completed -> OfframpStep.WAITING_FOR_COMPLETION
    is OfframpStatus.Failed -> this.step
}

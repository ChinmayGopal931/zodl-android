package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.evm.abi.Selector4
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.offramp.p2p.OrderStatus
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigInteger

sealed class OfframpStatus {
    object Idle : OfframpStatus()

    data class SelectingCircle(
        val candidateCount: Int,
        val selectedCircleId: BigInteger? = null,
    ) : OfframpStatus()

    data class ApprovingUsdc(
        val txHash: TxHash,
        val amount: Usdc6,
    ) : OfframpStatus()

    data class PlacingOrder(
        val txHash: TxHash,
        val circleId: BigInteger,
        val amount: Usdc6,
    ) : OfframpStatus()

    data class WaitingForMerchantAcceptance(
        val orderId: BigInteger,
        val pollAttempts: Int = 0,
        val lastObservedStatus: OrderStatus? = null,
        /**
         * Set once we've been polling for [OfframpOrchestrator.stalledAfterMs] without a merchant
         * accepting. The order is still live on-chain — there is no client-side timeout. UI
         * surfaces a hint that the user's USDC stays escrowed until on-chain auto-cancel (~72h).
         */
        val stalled: Boolean = false,
    ) : OfframpStatus()

    data class SendingEncryptedUpi(
        val orderId: BigInteger,
        val txHash: TxHash,
        val merchantAddress: Address,
        val merchantPubKey: String,
        val acceptedAtEpochSeconds: Long? = null,
    ) : OfframpStatus()

    data class WaitingForCompletion(
        val orderId: BigInteger,
        val pollAttempts: Int = 0,
        val lastObservedStatus: OrderStatus? = null,
        /** Same semantics as [WaitingForMerchantAcceptance.stalled]. */
        val stalled: Boolean = false,
        val acceptedAtEpochSeconds: Long? = null,
        val paidAtEpochSeconds: Long? = null,
    ) : OfframpStatus()

    data class Completed(
        val orderId: BigInteger,
        val acceptedMerchant: Address,
        val actualUsdcAmount: Usdc6? = null,
        val actualFiatAmount: Usdc6? = null,
        val placedAtEpochSeconds: Long? = null,
        val acceptedAtEpochSeconds: Long? = null,
        val paidAtEpochSeconds: Long? = null,
        val completedAtEpochSeconds: Long? = null,
    ) : OfframpStatus()

    /**
     * Order observed in on-chain CANCELLED status. This is a normal terminal state — the contract
     * auto-cancels orders that don't progress within its expiry window (~72h per
     * Diamond.getOrderExpiryTime), and the USDC has been refunded to the offramp account on-chain.
     * Distinct from [Failed], which signals a genuine error (revert, RPC death, etc.).
     */
    data class Cancelled(
        val orderId: BigInteger,
        val cancelledAtEpochSeconds: Long?,
        val acceptedMerchant: Address? = null,
        val refundedUsdcAmount: Usdc6? = null,
        val placedAtEpochSeconds: Long? = null,
        val acceptedAtEpochSeconds: Long? = null,
        val paidAtEpochSeconds: Long? = null,
    ) : OfframpStatus()

    data class Failed(
        val message: String,
        val orderId: BigInteger?,
        val step: OfframpStep,
        val txHash: TxHash? = null,
        val revertSelector: Selector4? = null,
        val knownRevertReason: KnownRevertReason? = null,
        /**
         * Long-tail SDK error name from [KnownContractErrors] when [knownRevertReason] is null but
         * the selector is one the SDK recognises. Lets the UI render "Contract error: FOO" instead
         * of dumping the raw 4-byte selector for the ~115 errors we don't curate.
         */
        val sdkErrorName: String? = null,
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
    is OfframpStatus.Cancelled -> OfframpStep.WAITING_FOR_COMPLETION
    is OfframpStatus.Failed -> this.step
}

package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.offramp.p2p.OrderStatus
import java.math.BigInteger

/**
 * State emitted by [OfframpOrchestrator]. Each non-terminal state surfaces every datum we
 * have available at that step — tx hashes, on-chain values, poll counters — so the UI can
 * render the most informative progress view possible without re-querying.
 */
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
        val merchantAddress: String,
        val merchantPubKey: String,
    ) : OfframpStatus()

    data class WaitingForCompletion(
        val orderId: BigInteger,
        val pollAttempts: Int = 0,
        val lastObservedStatus: OrderStatus? = null,
    ) : OfframpStatus()

    data class Completed(
        val orderId: BigInteger,
        val acceptedMerchant: String,
        val actualUsdcAmount: BigInteger? = null,
        val actualFiatAmount: BigInteger? = null,
        val completedAtEpochSeconds: Long? = null,
    ) : OfframpStatus()

    data class Failed(
        val message: String,
        val orderId: BigInteger?,
        val step: FailedStep,
        val txHash: String? = null,
        val revertSelector: String? = null,
        val decodedReason: String? = null,
        val cause: Throwable? = null,
    ) : OfframpStatus()
}

/** Which step of the offramp pipeline failed. Used to render the failure header in UI. */
enum class FailedStep {
    INITIALIZATION,
    SELECTING_CIRCLE,
    APPROVING_USDC,
    PLACING_ORDER,
    WAITING_FOR_ACCEPTANCE,
    ENCRYPTING_UPI,
    SENDING_UPI,
    WAITING_FOR_COMPLETION,
}

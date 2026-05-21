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
        val step: FailedStep,
        val txHash: String? = null,
        val revertSelector: Selector4? = null,
        val knownRevertReason: KnownRevertReason? = null,
        val solidityErrorString: String? = null,
        val cause: Throwable? = null,
    ) : OfframpStatus()
}

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

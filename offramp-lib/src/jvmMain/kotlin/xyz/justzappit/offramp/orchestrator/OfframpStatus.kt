package xyz.justzappit.offramp.orchestrator

import java.math.BigInteger

sealed class OfframpStatus {
    object Idle : OfframpStatus()
    data class SelectingCircle(val candidateCount: Int) : OfframpStatus()
    data class ApprovingUsdc(val txHash: String) : OfframpStatus()
    data class PlacingOrder(val txHash: String) : OfframpStatus()
    data class WaitingForMerchantAcceptance(val orderId: BigInteger) : OfframpStatus()
    data class SendingEncryptedUpi(val orderId: BigInteger, val txHash: String) : OfframpStatus()
    data class WaitingForCompletion(val orderId: BigInteger) : OfframpStatus()
    data class Completed(val orderId: BigInteger) : OfframpStatus()
    data class Failed(val message: String, val orderId: BigInteger?, val cause: Throwable?) : OfframpStatus()
}

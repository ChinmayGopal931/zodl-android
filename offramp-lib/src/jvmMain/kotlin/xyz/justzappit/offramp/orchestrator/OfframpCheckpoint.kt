package xyz.justzappit.offramp.orchestrator

import kotlinx.serialization.Serializable
import xyz.justzappit.offramp.p2p.CurrencyCode
import java.math.BigInteger

/**
 * Resumable snapshot of an in-flight offramp order. Persisted after every on-chain checkpoint so
 * that process death between broadcasts doesn't orphan the user's USDC. BigInteger fields are
 * serialised as decimal strings so the wire format stays human-readable in encrypted preferences.
 */
@Serializable
data class OfframpCheckpoint(
    val orderId: String?,
    val currentStep: OfframpStep,
    val approveTxHash: String? = null,
    val placeOrderTxHash: String? = null,
    val setUpiTxHash: String? = null,
    val recipientUpi: String,
    val usdcAmountMicroDecimal: String,
    val currency: CurrencyCode,
    val createdAtMillis: Long,
) {
    val orderIdBig: BigInteger? get() = orderId?.let(::BigInteger)
    val usdcAmountMicro: BigInteger get() = BigInteger(usdcAmountMicroDecimal)

    fun toRequest(): OfframpRequest = OfframpRequest(
        recipientUpi = recipientUpi,
        usdcAmount = usdcAmountMicro,
        currency = currency,
    )
}

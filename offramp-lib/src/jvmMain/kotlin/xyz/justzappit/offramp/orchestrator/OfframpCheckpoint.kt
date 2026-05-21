package xyz.justzappit.offramp.orchestrator

import kotlinx.serialization.Serializable
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigInteger

/**
 * Resumable snapshot of an in-flight offramp order. Persisted after every on-chain checkpoint so
 * that process death between broadcasts doesn't orphan the user's USDC.
 *
 * Wire format intentionally stores the orderId + amount as decimal strings: encrypted preferences
 * are human-readable enough to triage and we don't want a serialization-library upgrade silently
 * changing how `BigInteger`/`Usdc6` round-trip. [orderIdBig] / [usdcAmount] convert on read; the
 * primary constructor validates eagerly so a corrupt blob fails at decode time, not later inside
 * the resume path.
 */
@Serializable
data class OfframpCheckpoint(
    val orderId: String?,
    val currentStep: OfframpStep,
    val approveTxHash: TxHash? = null,
    val placeOrderTxHash: TxHash? = null,
    val setUpiTxHash: TxHash? = null,
    val recipientUpi: String,
    val usdcAmountMicroDecimal: String,
    val currency: CurrencyCode,
    val createdAtMillis: Long,
) {
    init {
        // Validate at construction so deserialization (which uses this same constructor) surfaces
        // a corrupt blob immediately rather than during the next resume() call.
        if (orderId != null) {
            require(runCatching { BigInteger(orderId) }.isSuccess) {
                "OfframpCheckpoint.orderId must be a decimal integer, got '$orderId'"
            }
        }
        require(runCatching { BigInteger(usdcAmountMicroDecimal) }.isSuccess) {
            "OfframpCheckpoint.usdcAmountMicroDecimal must be a decimal integer, got '$usdcAmountMicroDecimal'"
        }
    }

    val orderIdBig: BigInteger? get() = orderId?.let(::BigInteger)
    val usdcAmount: Usdc6 get() = Usdc6(BigInteger(usdcAmountMicroDecimal))

    fun toRequest(): OfframpRequest = OfframpRequest(
        recipientUpi = recipientUpi,
        usdcAmount = usdcAmount,
        currency = currency,
    )
}

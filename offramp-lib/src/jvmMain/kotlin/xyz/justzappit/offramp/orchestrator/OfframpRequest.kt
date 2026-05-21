package xyz.justzappit.offramp.orchestrator

import java.math.BigInteger

data class OfframpRequest(
    val recipientUpi: String,
    val usdcAmount: BigInteger,
    val currency: String = "INR",
) {
    init {
        require(recipientUpi.isNotBlank()) { "recipientUpi must not be blank" }
        require(usdcAmount > BigInteger.ZERO) { "usdcAmount must be positive" }
        require(currency.isNotBlank()) { "currency must not be blank" }
    }
}

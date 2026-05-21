package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.offramp.p2p.CurrencyCode
import java.math.BigInteger

data class OfframpRequest(
    val recipientUpi: String,
    val usdcAmount: BigInteger,
    val currency: CurrencyCode = CurrencyCode.Inr,
) {
    init {
        require(recipientUpi.isNotBlank()) { "recipientUpi must not be blank" }
        require(usdcAmount > BigInteger.ZERO) { "usdcAmount must be positive" }
    }
}

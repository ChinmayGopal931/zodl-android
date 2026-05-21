package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.Usdc6

data class OfframpRequest(
    val recipientUpi: String,
    val usdcAmount: Usdc6,
    val currency: CurrencyCode = CurrencyCode.Inr,
) {
    init {
        require(recipientUpi.isNotBlank()) { "recipientUpi must not be blank" }
        require(usdcAmount > Usdc6.ZERO) { "usdcAmount must be positive" }
    }
}

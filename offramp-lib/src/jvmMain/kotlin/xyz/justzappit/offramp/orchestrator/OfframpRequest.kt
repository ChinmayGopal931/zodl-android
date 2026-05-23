package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.Usdc6

data class OfframpRequest(
    val recipientUpi: String,
    val usdcAmount: Usdc6,
    val currency: CurrencyCode = CurrencyCode.Inr,
    /**
     * Minimum fiat the order must yield, passed to the Diamond as `placeOrder._fiatAmountLimit` —
     * the contract's slippage floor (reverts `SLIPPAGE_EXCEEDED` below it). `null` (or [Usdc6.ZERO])
     * disables the check. The UI computes this from the quoted INR rate with a tolerance so a rate
     * drift across the (multi-minute) funding bridge can't silently pay the user less than quoted.
     */
    val minFiatAmount: Usdc6? = null,
) {
    init {
        require(recipientUpi.isNotBlank()) { "recipientUpi must not be blank" }
        require(usdcAmount > Usdc6.ZERO) { "usdcAmount must be positive" }
    }
}

package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigInteger

data class OfframpRequest(
    val recipientUpi: String,
    val usdcAmount: Usdc6,
    /**
     * User-quoted fiat in 6-decimal micros (e.g. `37_280_000` ⇒ ₹37.28). Goes into the `am=`
     * field of the `upi://pay?…` URI for setSellOrderUpi. Required: a zero `am` breaks the
     * SDK-side `parseAmount` and triggers the +4s merchant auto-cancel (§6 of findings).
     */
    val fiatAmount: Usdc6,
    val currency: CurrencyCode = CurrencyCode.Inr,
    /** Optional `pn=` display name. */
    val payeeName: String? = null,
    /**
     * `placeOrder._fiatAmountLimit`: contract slippage floor. Null/ZERO disables it; the UI sets
     * `floor(fiatAmount × 0.99)` so rate drift across the funding bridge can't underpay the user.
     */
    val fiatAmountLimit: Usdc6? = null,
) {
    init {
        require(recipientUpi.isNotBlank()) { "recipientUpi must not be blank" }
        require(usdcAmount > Usdc6.ZERO) { "usdcAmount must be positive" }
        require(fiatAmount > Usdc6.ZERO) { "fiatAmount must be positive" }
    }

    companion object {
        private val SLIPPAGE_FLOOR_BASIS_POINTS = BigInteger.valueOf(9_900)
        private val BASIS_POINTS_DENOMINATOR = BigInteger.valueOf(10_000)

        /**
         * Contract slippage floor for [fiatAmount]: `floor(fiatAmount × 0.99)`, so rate drift across
         * the funding bridge can't underpay the user. Non-positive input → ZERO (floor disabled).
         */
        fun slippageFloor(fiatAmount: Usdc6): Usdc6 =
            if (fiatAmount.micros.signum() <= 0) {
                Usdc6.ZERO
            } else {
                Usdc6(fiatAmount.micros.multiply(SLIPPAGE_FLOOR_BASIS_POINTS).divide(BASIS_POINTS_DENOMINATOR))
            }
    }
}

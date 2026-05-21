package co.electriccoin.zcash.ui.screen.swap.upi.progress

import kotlinx.serialization.Serializable
import xyz.justzappit.offramp.p2p.CurrencyCode

@Serializable
data class UpiOfframpProgressArgs(
    val recipientUpi: String,
    val usdcAmountMicro: String,
    val currency: CurrencyCode,
)

package co.electriccoin.zcash.ui.screen.swap.upi.progress

import kotlinx.serialization.Serializable

@Serializable
data class UpiOfframpProgressArgs(
    val recipientUpi: String,
    val usdcAmountMicro: String,
    val currency: String,
)

package co.electriccoin.zcash.ui.screen.swap.upi.scan

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ScanUpiArgs(
    val requestId: String = UUID.randomUUID().toString(),
)

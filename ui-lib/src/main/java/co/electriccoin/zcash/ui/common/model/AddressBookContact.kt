package co.electriccoin.zcash.ui.common.model

import kotlin.time.Instant

data class AddressBookContact(
    val name: String,
    val address: String,
    val lastUpdated: Instant,
    val chain: String?,
    val walletAddresses: Map<String, String> = emptyMap(),
) {
    companion object {
        const val ADDR_TYPE_UNIFIED = "zcash_unified"
        const val ADDR_TYPE_TRANSPARENT = "zcash_transparent"
        const val ADDR_TYPE_EVM = "evm"
        const val ADDR_TYPE_SOLANA = "solana"

        val SUPPORTED_ADDR_TYPES =
            listOf(
                ADDR_TYPE_UNIFIED,
                ADDR_TYPE_TRANSPARENT,
                ADDR_TYPE_EVM,
                ADDR_TYPE_SOLANA,
            )
    }
}

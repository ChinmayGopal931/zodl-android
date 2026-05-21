package xyz.justzappit.evm.rpc

import kotlinx.serialization.Serializable

@Serializable
data class TransactionReceipt(
    val transactionHash: String,
    val blockNumber: String,
    val status: String,
    val gasUsed: String,
    val effectiveGasPrice: String? = null,
    val contractAddress: String? = null,
    val logs: List<EvmLog> = emptyList(),
) {
    val success: Boolean get() = status == "0x1"
}

@Serializable
data class EvmLog(
    val address: String,
    val topics: List<String>,
    val data: String,
    val blockNumber: String,
    val transactionHash: String,
    val logIndex: String,
    val removed: Boolean = false,
)

@Serializable
data class BlockHeader(
    val number: String,
    val timestamp: String,
    val baseFeePerGas: String? = null,
)

package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiAddress
import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.rpc.EvmLog
import xyz.justzappit.evm.rpc.TransactionReceipt
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger

object OrderEvents {
    val ORDER_PLACED_TOPIC: String =
        "0x" +
            keccak256(
                ORDER_PLACED_CANONICAL_SIGNATURE.toByteArray(Charsets.US_ASCII),
            ).toHex()

    fun parseOrderIdFromReceipt(
        receipt: TransactionReceipt,
        diamondAddress: Address,
        userAddress: Address,
    ): BigInteger? {
        val userTopic = padAddressTopic(userAddress)
        val diamondHex = diamondAddress.lowercaseHex
        val candidate =
            receipt.logs.firstOrNull { log ->
                log.address.equals(diamondHex, ignoreCase = true) &&
                    log.topics.size >= REQUIRED_TOPICS &&
                    log.topics[0].equals(ORDER_PLACED_TOPIC, ignoreCase = true) &&
                    log.topics[2].equals(userTopic, ignoreCase = true)
            } ?: receipt.logs.firstOrNull { log ->
                log.address.equals(diamondHex, ignoreCase = true) &&
                    log.topics.size >= REQUIRED_TOPICS &&
                    log.topics[0].equals(ORDER_PLACED_TOPIC, ignoreCase = true)
            }
        return candidate?.let { topicToBigInteger(it.topics[1]) }
    }

    fun parseOrderIdFromLog(log: EvmLog): BigInteger? {
        if (log.topics.firstOrNull()?.equals(ORDER_PLACED_TOPIC, ignoreCase = true) != true) return null
        if (log.topics.size < INDEXED_PARAMS + 1) return null
        return topicToBigInteger(log.topics[1])
    }

    private fun topicToBigInteger(topic: String): BigInteger = BigInteger(1, topic.hexToBytes())

    // An indexed `address` event arg is ABI-encoded as a left-padded 32-byte word — exactly what
    // AbiAddress.head() produces.
    private fun padAddressTopic(address: Address): String = "0x" + AbiAddress(address).head().toHex()

    private const val INDEXED_PARAMS = 3
    private const val REQUIRED_TOPICS = INDEXED_PARAMS + 1

    // Mirrors OrderPlaced from p2pdotme-sdk's order-flow-facet ABI.
    private const val ORDER_PLACED_CANONICAL_SIGNATURE =
        "OrderPlaced(uint256,address,address,uint256,uint8,uint256," +
            "(uint256,uint256,uint256,uint256,uint256,address,address,address," +
            "string,string,bool,uint8,uint8,(uint8,uint8,uint256,uint256)," +
            "uint256,string,string,uint256,uint256[],bytes32,uint256,uint256))"
}

package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.keccak256
import xyz.justzappit.evm.rpc.EvmLog
import xyz.justzappit.evm.rpc.TransactionReceipt
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger

object OrderEvents {
    val ORDER_PLACED_TOPIC: String = "0x" + keccak256(
        ORDER_PLACED_CANONICAL_SIGNATURE.toByteArray(Charsets.US_ASCII),
    ).toHex()

    fun parseOrderIdFromReceipt(
        receipt: TransactionReceipt,
        diamondAddress: String,
        userAddress: String,
    ): BigInteger? {
        val userTopic = padAddressTopic(userAddress)
        val candidate = receipt.logs.firstOrNull { log ->
            log.address.equals(diamondAddress, ignoreCase = true) &&
                log.topics.size >= REQUIRED_TOPICS &&
                log.topics[0].equals(ORDER_PLACED_TOPIC, ignoreCase = true) &&
                log.topics[2].equals(userTopic, ignoreCase = true)
        } ?: receipt.logs.firstOrNull { log ->
            log.address.equals(diamondAddress, ignoreCase = true) &&
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

    private fun topicToBigInteger(topic: String): BigInteger =
        BigInteger(1, topic.removePrefix("0x").chunked(2).map { it.toInt(16).toByte() }.toByteArray())

    private fun padAddressTopic(address: String): String {
        val raw = address.removePrefix("0x").lowercase()
        require(raw.length == ADDRESS_HEX_LEN) { "address must be 20 bytes hex, got '$address'" }
        return "0x" + "0".repeat(TOPIC_HEX_LEN - ADDRESS_HEX_LEN) + raw
    }

    private const val ADDRESS_HEX_LEN = 40
    private const val TOPIC_HEX_LEN = 64
    private const val INDEXED_PARAMS = 3
    private const val REQUIRED_TOPICS = INDEXED_PARAMS + 1

    // Mirrors OrderPlaced from p2pdotme-sdk's order-flow-facet ABI.
    private const val ORDER_PLACED_CANONICAL_SIGNATURE =
        "OrderPlaced(uint256,address,address,uint256,uint8,uint256," +
            "(uint256,uint256,uint256,uint256,uint256,address,address,address," +
            "string,string,bool,uint8,uint8,(uint8,uint8,uint256,uint256)," +
            "uint256,string,string,uint256,uint256[],bytes32,uint256,uint256))"
}

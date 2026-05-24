package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.rpc.EvmLog
import xyz.justzappit.evm.rpc.TransactionReceipt
import xyz.justzappit.evm.types.Address
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderEventsTest {
    @Test
    fun `parseOrderIdFromReceipt picks the matching user's log`() {
        val diamond = Address.parse("0xce868398fdadca368eac203222874d6888532ae2")
        val user = Address.parse("0x9858effd232b4033e47d90003d41ec34ecaeda94")
        val otherUser = Address.parse("0x000000000000000000000000000000000000beef")

        val orderId = BigInteger.valueOf(123)
        val orderIdTopic = orderId.toString(16).padStart(64, '0').let { "0x$it" }

        val log1 =
            sampleLog(
                diamond.lowercaseHex,
                topics =
                    listOf(
                        OrderEvents.ORDER_PLACED_TOPIC,
                        orderIdTopic,
                        addressAsTopic(otherUser.lowercaseHex),
                        addressAsTopic(diamond.lowercaseHex),
                    ),
            )
        val log2 =
            sampleLog(
                diamond.lowercaseHex,
                topics =
                    listOf(
                        OrderEvents.ORDER_PLACED_TOPIC,
                        orderIdTopic,
                        addressAsTopic(user.lowercaseHex),
                        addressAsTopic(diamond.lowercaseHex),
                    ),
            )

        val receipt = sampleReceipt(logs = listOf(log1, log2))
        assertEquals(orderId, OrderEvents.parseOrderIdFromReceipt(receipt, diamond, user))
    }

    @Test
    fun `parseOrderIdFromReceipt falls back to first matching event when no user match`() {
        val diamond = Address.parse("0xce868398fdadca368eac203222874d6888532ae2")
        val orderId = BigInteger.valueOf(7)
        val orderIdTopic = "0x" + orderId.toString(16).padStart(64, '0')

        val log =
            sampleLog(
                diamond.lowercaseHex,
                topics =
                    listOf(
                        OrderEvents.ORDER_PLACED_TOPIC,
                        orderIdTopic,
                        addressAsTopic("0x000000000000000000000000000000000000beef"),
                        addressAsTopic(diamond.lowercaseHex),
                    ),
            )
        val receipt = sampleReceipt(logs = listOf(log))
        // user not in any topic; falls back to first matching event
        val unknownUser = Address.parse("0x000000000000000000000000000000000000cafe")
        assertEquals(orderId, OrderEvents.parseOrderIdFromReceipt(receipt, diamond, unknownUser))
    }

    @Test
    fun `parseOrderIdFromReceipt returns null when no OrderPlaced log`() {
        val diamond = Address.parse("0xce868398fdadca368eac203222874d6888532ae2")
        val user = Address.parse("0x9858effd232b4033e47d90003d41ec34ecaeda94")
        val log = sampleLog(diamond.lowercaseHex, topics = listOf("0x" + "ff".repeat(32)))
        val receipt = sampleReceipt(logs = listOf(log))
        assertNull(OrderEvents.parseOrderIdFromReceipt(receipt, diamond, user))
    }

    @Test
    fun `parseOrderIdFromReceipt ignores malformed logs with too few topics`() {
        val diamond = Address.parse("0xce868398fdadca368eac203222874d6888532ae2")
        val user = Address.parse("0x9858effd232b4033e47d90003d41ec34ecaeda94")
        val orderId = BigInteger.valueOf(99)
        val orderIdTopic = "0x" + orderId.toString(16).padStart(64, '0')
        // Topic[0] matches OrderPlaced but only one topic — no orderId at topics[1].
        val malformed = sampleLog(diamond.lowercaseHex, topics = listOf(OrderEvents.ORDER_PLACED_TOPIC))
        // A second well-formed log later in the receipt should still resolve.
        val wellFormed =
            sampleLog(
                diamond.lowercaseHex,
                topics =
                    listOf(
                        OrderEvents.ORDER_PLACED_TOPIC,
                        orderIdTopic,
                        addressAsTopic(user.lowercaseHex),
                        addressAsTopic(diamond.lowercaseHex),
                    ),
            )
        val receipt = sampleReceipt(logs = listOf(malformed, wellFormed))
        assertEquals(orderId, OrderEvents.parseOrderIdFromReceipt(receipt, diamond, user))
    }

    @Test
    fun `parseOrderIdFromReceipt ignores logs from other contracts`() {
        val diamond = Address.parse("0xce868398fdadca368eac203222874d6888532ae2")
        val other = Address.parse("0x0000000000000000000000000000000000000bad")
        val user = Address.parse("0x9858effd232b4033e47d90003d41ec34ecaeda94")
        val orderId = BigInteger.valueOf(42)
        val orderIdTopic = "0x" + orderId.toString(16).padStart(64, '0')
        val log =
            sampleLog(
                other.lowercaseHex,
                topics =
                    listOf(
                        OrderEvents.ORDER_PLACED_TOPIC,
                        orderIdTopic,
                        addressAsTopic(user.lowercaseHex),
                        addressAsTopic(diamond.lowercaseHex),
                    ),
            )
        val receipt = sampleReceipt(logs = listOf(log))
        assertNull(OrderEvents.parseOrderIdFromReceipt(receipt, diamond, user))
    }

    @Test
    fun `parseOrderIdFromLog extracts uint256 from topics 1`() {
        val orderId = BigInteger.valueOf(99)
        val log =
            sampleLog(
                "0xdiamond",
                topics =
                    listOf(
                        OrderEvents.ORDER_PLACED_TOPIC,
                        "0x" + orderId.toString(16).padStart(64, '0'),
                        "0x" + "00".repeat(32),
                        "0x" + "00".repeat(32),
                    ),
            )
        assertEquals(orderId, OrderEvents.parseOrderIdFromLog(log))
    }

    private fun sampleLog(address: String, topics: List<String>) =
        EvmLog(
            address = address,
            topics = topics,
            data = "0x",
            blockNumber = "0x1",
            transactionHash = "0xtx",
            logIndex = "0x0",
        )

    private fun sampleReceipt(logs: List<EvmLog>) =
        TransactionReceipt(
            transactionHash = "0xtx",
            blockNumber = "0x1",
            status = "0x1",
            gasUsed = "0x5208",
            logs = logs,
        )

    private fun addressAsTopic(address: String): String {
        val raw = address.removePrefix("0x").lowercase()
        return "0x" + "0".repeat(64 - raw.length) + raw
    }
}

package xyz.justzappit.offramp.orchestrator

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.justzappit.evm.hd.EvmKeyDerivation
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.signer.EoaSigner
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import xyz.justzappit.offramp.config.P2pNetworks
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.OrderEvents
import xyz.justzappit.offramp.p2p.OrderStatus
import xyz.justzappit.offramp.p2p.SubgraphClient
import java.math.BigInteger
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfframpOrchestratorTest {

    private val account = EvmKeyDerivation.derive(MNEMONIC, accountIndex = 0)
    private val network = P2pNetworks.SEPOLIA

    private val rpcRequestLog = mutableListOf<String>()
    private val rawTxLog = mutableListOf<String>()
    private var getOrdersByIdResponses = ArrayDeque<String>()
    private var getAssignableResponse = ENCODED_ADDRESS_ARRAY_OF_ONE

    private val rpcEngine = MockEngine { request ->
        val bytes = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes()
        val payload = Json.parseToJsonElement(bytes.decodeToString()) as JsonObject
        val method = payload["method"]!!.jsonPrimitive.content
        rpcRequestLog += method
        val body = when (method) {
            "eth_getTransactionCount" -> """{"jsonrpc":"2.0","id":1,"result":"0x01"}"""
            "eth_maxPriorityFeePerGas" -> """{"jsonrpc":"2.0","id":1,"result":"0x3b9aca00"}"""
            "eth_getBlockByNumber" ->
                """{"jsonrpc":"2.0","id":1,"result":{"number":"0x100","timestamp":"0x68000000","baseFeePerGas":"0x77359400"}}"""
            "eth_estimateGas" -> """{"jsonrpc":"2.0","id":1,"result":"0x5208"}"""
            "eth_sendRawTransaction" -> {
                rawTxLog += payload["params"]!!.toString().substringAfter('"').substringBefore('"')
                """{"jsonrpc":"2.0","id":1,"result":"0xtx${rawTxLog.size}"}"""
            }
            "eth_getTransactionReceipt" -> receiptFor(payload)
            "eth_call" -> ethCallResponse(payload)
            else -> error("Unexpected RPC method: $method")
        }
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private var nextSubgraphResponse = SUBGRAPH_OK_ONE_CIRCLE
    private val subgraphEngine = MockEngine {
        respond(nextSubgraphResponse, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private val rpcHttp = HttpClient(rpcEngine) { install(ContentNegotiation) { json() } }
    private val subgraphHttp = HttpClient(subgraphEngine) { install(ContentNegotiation) { json() } }
    private val rpc = BaseRpcClient(rpcHttp, "http://mock/rpc")
    private val signer = EoaSigner(rpc, chainId = network.chainId, account = account)
    private val subgraph = SubgraphClient(subgraphHttp, "http://mock/graph")
    private val orchestrator = OfframpOrchestrator(
        rpc = rpc,
        signer = signer,
        account = account,
        network = network,
        subgraph = subgraph,
        router = CircleRouter(random = Random(0), epsilon = 0.0),
        pollIntervalMs = 0,
        acceptanceTimeoutMs = 5_000,
        completionTimeoutMs = 5_000,
    )

    @AfterTest
    fun shutdown() {
        rpcHttp.close()
        subgraphHttp.close()
    }

    @Test
    fun `happy path emits the full status sequence and ends in Completed`() = runTest {
        getOrdersByIdResponses = ArrayDeque(
            listOf(
                synthOrderHex(status = OrderStatus.ACCEPTED, pubkey = MERCHANT_PUBKEY),
                synthOrderHex(status = OrderStatus.COMPLETED, pubkey = MERCHANT_PUBKEY),
            ),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = BigInteger.valueOf(5_000_000),
                currency = "INR",
            ),
        ).toList()

        assertIs<OfframpStatus.Idle>(statuses.first())
        assertIs<OfframpStatus.Completed>(statuses.last())
        val completed = statuses.last() as OfframpStatus.Completed
        assertEquals(ORDER_ID, completed.orderId)

        // Sequence sanity — every important state showed up in order.
        val classes = statuses.map { it::class.simpleName }
        assertTrue(
            classes.indexOf("Idle") <
                classes.indexOf("SelectingCircle"),
        )
        assertTrue(
            classes.indexOf("SelectingCircle") <
                classes.indexOf("ApprovingUsdc"),
        )
        assertTrue(
            classes.indexOf("ApprovingUsdc") <
                classes.indexOf("PlacingOrder"),
        )
        assertTrue(
            classes.indexOf("PlacingOrder") <
                classes.indexOf("WaitingForMerchantAcceptance"),
        )
        assertTrue(
            classes.indexOf("WaitingForMerchantAcceptance") <
                classes.indexOf("SendingEncryptedUpi"),
        )
        assertTrue(
            classes.indexOf("SendingEncryptedUpi") <
                classes.indexOf("WaitingForCompletion"),
        )
        assertTrue(
            classes.indexOf("WaitingForCompletion") <
                classes.indexOf("Completed"),
        )

        // 3 broadcasts: approve, placeOrder, setSellOrderUpi.
        assertEquals(3, rawTxLog.size, "expected 3 broadcasts, got ${rawTxLog.size}")
    }

    @Test
    fun `subgraph returning no circles surfaces as Failed`() = runTest {
        nextSubgraphResponse = """{"data":{"circles":[]}}"""

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = BigInteger.valueOf(5_000_000),
            ),
        ).toList()
        val last = statuses.last()
        assertIs<OfframpStatus.Failed>(last)
        assertEquals(null, last.orderId)
    }

    @Test
    fun `merchant cancels order after acceptance window opens - reported as Failed`() = runTest {
        getOrdersByIdResponses = ArrayDeque(
            listOf(synthOrderHex(status = OrderStatus.CANCELLED, pubkey = "")),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = BigInteger.valueOf(5_000_000),
            ),
        ).toList()
        val last = statuses.last()
        assertIs<OfframpStatus.Failed>(last)
        assertEquals(ORDER_ID, last.orderId)
        assertTrue(last.message.contains("cancelled", ignoreCase = true))
    }

    private fun receiptFor(payload: JsonObject): String {
        // All broadcasts succeed and emit an OrderPlaced event for txHash matching placeOrder
        val txParam = payload["params"]!!.toString().substringAfter('"').substringBefore('"')
        return when (txParam) {
            "0xtx2" -> placeOrderReceiptJson() // second broadcast = placeOrder
            else -> simpleSuccessReceiptJson(txParam)
        }
    }

    private fun ethCallResponse(payload: JsonObject): String {
        // The data field's first 4 bytes identify the function selector.
        val params = payload["params"]!!.toString()
        return when {
            // 0xcea99cd6 = getOrdersById
            params.contains("0xcea99cd6") -> {
                val next = getOrdersByIdResponses.removeFirst()
                """{"jsonrpc":"2.0","id":1,"result":"$next"}"""
            }
            // 0x36b0ec9a = getAssignableMerchantsFromCircle
            params.contains("0x36b0ec9a") ->
                """{"jsonrpc":"2.0","id":1,"result":"$getAssignableResponse"}"""
            else -> error("Unexpected eth_call: $params")
        }
    }

    private fun simpleSuccessReceiptJson(txHash: String): String = """
        {"jsonrpc":"2.0","id":1,"result":{
          "transactionHash":"$txHash",
          "blockNumber":"0x10",
          "status":"0x1",
          "gasUsed":"0x5208",
          "logs":[]
        }}
    """.trimIndent()

    private fun placeOrderReceiptJson(): String {
        val userTopic = "0x" + "0".repeat(24) + account.address.removePrefix("0x").lowercase()
        val orderIdTopic = "0x" + ORDER_ID.toString(16).padStart(64, '0')
        return """
            {"jsonrpc":"2.0","id":1,"result":{
              "transactionHash":"0xtx2",
              "blockNumber":"0x10",
              "status":"0x1",
              "gasUsed":"0x5208",
              "logs":[
                {
                  "address":"${network.diamondAddress.lowercase()}",
                  "topics":["${OrderEvents.ORDER_PLACED_TOPIC}",
                            "$orderIdTopic",
                            "$userTopic",
                            "0x${"0".repeat(64)}"],
                  "data":"0x",
                  "blockNumber":"0x10",
                  "transactionHash":"0xtx2",
                  "logIndex":"0x0"
                }
              ]
            }}
        """.trimIndent()
    }

    /** Builds a synthetic Order struct encoded as eth_call would return it. */
    private fun synthOrderHex(status: OrderStatus, pubkey: String): String {
        val WORD = 32
        val headSlots = 25
        val tupleHead = ByteArray(headSlots * WORD)

        // Slot 5: acceptedMerchant — set when ACCEPTED+
        if (status.onChain >= OrderStatus.ACCEPTED.onChain) {
            val merchantBytes = "11".repeat(20).hexToBytes()
            System.arraycopy(merchantBytes, 0, tupleHead, 5 * WORD + (WORD - 20), 20)
        }

        // Slot 11: status (uint8) — last byte of slot
        tupleHead[12 * WORD - 1] = status.onChain.toByte()

        // Slot 8: pubkey offset
        val tupleTail: ByteArray
        if (pubkey.isNotEmpty()) {
            val tailOffset = headSlots * WORD
            val offsetBytes = BigInteger.valueOf(tailOffset.toLong()).toByteArray()
            System.arraycopy(offsetBytes, 0, tupleHead, 9 * WORD - offsetBytes.size, offsetBytes.size)
            val pubkeyBytes = pubkey.toByteArray(Charsets.UTF_8)
            val pad = if (pubkeyBytes.size % WORD == 0) 0 else WORD - (pubkeyBytes.size % WORD)
            val tail = ByteArray(WORD + pubkeyBytes.size + pad)
            val lenBytes = BigInteger.valueOf(pubkeyBytes.size.toLong()).toByteArray()
            System.arraycopy(lenBytes, 0, tail, WORD - lenBytes.size, lenBytes.size)
            System.arraycopy(pubkeyBytes, 0, tail, WORD, pubkeyBytes.size)
            tupleTail = tail
        } else {
            tupleTail = ByteArray(0)
        }

        // Top-level offset = 0x20
        val topOffset = ByteArray(WORD).also { it[WORD - 1] = 0x20.toByte() }
        val all = topOffset + tupleHead + tupleTail
        return "0x" + all.toHex()
    }

    companion object {
        const val MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about"
        const val MERCHANT_PUBKEY =
            "1b84c5567b126440995d3ed5aaba0565d71e1834604819ff9c17f5e9d5dd078f" +
                "70beaf8f588b541507fed6a642c5ab42dfdf8120a7f639de5122d47a69a8e8d1"
        val ORDER_ID: BigInteger = BigInteger.valueOf(7)

        const val ENCODED_ADDRESS_ARRAY_OF_ONE =
            "0x0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000111111111111111111111111111111111111baaf"

        const val SUBGRAPH_OK_ONE_CIRCLE = """
            {"data":{"circles":[
              {"circleId":"1",
               "currency":"0x494e520000000000000000000000000000000000000000000000000000000000",
               "metrics":{"circleScore":"50","circleStatus":"active",
                 "scoreState":{"activeMerchantsCount":"4"}}}
            ]}}
        """
    }
}


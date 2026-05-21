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
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.config.P2pNetworks
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.OrderEvents
import xyz.justzappit.offramp.p2p.OrderReadSource
import xyz.justzappit.offramp.p2p.OrderSnapshot
import xyz.justzappit.offramp.p2p.OrderStatus
import xyz.justzappit.offramp.p2p.OrderType
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
    private val orderReader = ScriptedOrderReadSource()
    private val orchestrator = OfframpOrchestrator(
        rpc = rpc,
        signer = signer,
        account = account,
        network = network,
        subgraph = subgraph,
        orderReader = orderReader,
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
        orderReader.enqueue(
            snapshot(status = OrderStatus.ACCEPTED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS),
            snapshot(
                status = OrderStatus.COMPLETED,
                pubkey = MERCHANT_PUBKEY,
                merchant = MERCHANT_ADDRESS,
                actualFiatAmount = BigInteger.valueOf(445_000_000),
                actualUsdcAmount = BigInteger.valueOf(5_062_500),
                completedAtEpochSeconds = 1_779_999_999L,
            ),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = BigInteger.valueOf(5_000_000),
                currency = CurrencyCode.Inr,
            ),
        ).toList()

        assertIs<OfframpStatus.Idle>(statuses.first())
        val completed = statuses.last() as OfframpStatus.Completed
        assertEquals(ORDER_ID, completed.orderId)
        assertEquals(BigInteger.valueOf(445_000_000), completed.actualFiatAmount)
        assertEquals(BigInteger.valueOf(5_062_500), completed.actualUsdcAmount)
        assertEquals(1_779_999_999L, completed.completedAtEpochSeconds)

        val classes = statuses.map { it::class.simpleName }
        assertTrue(classes.indexOf("Idle") < classes.indexOf("SelectingCircle"))
        assertTrue(classes.indexOf("SelectingCircle") < classes.indexOf("ApprovingUsdc"))
        assertTrue(classes.indexOf("ApprovingUsdc") < classes.indexOf("PlacingOrder"))
        assertTrue(classes.indexOf("PlacingOrder") < classes.indexOf("WaitingForMerchantAcceptance"))
        assertTrue(classes.indexOf("WaitingForMerchantAcceptance") < classes.indexOf("SendingEncryptedUpi"))
        assertTrue(classes.indexOf("SendingEncryptedUpi") < classes.indexOf("WaitingForCompletion"))
        assertTrue(classes.indexOf("WaitingForCompletion") < classes.indexOf("Completed"))

        // 3 broadcasts: approve, placeOrder, setSellOrderUpi.
        assertEquals(3, rawTxLog.size, "expected 3 broadcasts, got ${rawTxLog.size}")
    }

    @Test
    fun `subgraph returning no circles surfaces as Failed with no orderId and SELECTING_CIRCLE step`() = runTest {
        nextSubgraphResponse = """{"data":{"circles":[]}}"""

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = BigInteger.valueOf(5_000_000),
            ),
        ).toList()
        val last = assertIs<OfframpStatus.Failed>(statuses.last())
        assertEquals(null, last.orderId)
        assertEquals(OfframpStep.SELECTING_CIRCLE, last.step)
    }

    @Test
    fun `cancelled order returns Failed with orderId set + WAITING_FOR_ACCEPTANCE step`() = runTest {
        orderReader.enqueue(
            snapshot(status = OrderStatus.CANCELLED, pubkey = "", merchant = null),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = BigInteger.valueOf(5_000_000),
            ),
        ).toList()
        val last = assertIs<OfframpStatus.Failed>(statuses.last())
        assertEquals(ORDER_ID, last.orderId)
        assertEquals(OfframpStep.WAITING_FOR_ACCEPTANCE, last.step)
        assertTrue(last.message.contains("cancelled", ignoreCase = true))
    }

    private fun snapshot(
        status: OrderStatus,
        pubkey: String,
        merchant: String?,
        actualUsdcAmount: BigInteger? = null,
        actualFiatAmount: BigInteger? = null,
        completedAtEpochSeconds: Long? = null,
    ) = OrderSnapshot(
        orderId = ORDER_ID,
        status = status,
        orderType = OrderType.PAY,
        circleId = BigInteger.ONE,
        userAddress = account.address,
        usdcAmount = BigInteger.valueOf(5_000_000),
        fiatAmount = BigInteger.valueOf(445_000_000),
        currencyHex = "0x494e520000000000000000000000000000000000000000000000000000000000",
        acceptedMerchantAddress = merchant?.let { Address.parse(it) },
        merchantPubKey = pubkey,
        encryptedUserUpi = "",
        encryptedMerchantUpi = "",
        placedAtEpochSeconds = 1_779_000_000L,
        acceptedAtEpochSeconds = if (status.onChain >= OrderStatus.ACCEPTED.onChain) 1_779_500_000L else null,
        paidAtEpochSeconds = null,
        completedAtEpochSeconds = completedAtEpochSeconds,
        cancelledAtEpochSeconds = if (status == OrderStatus.CANCELLED) 1_779_500_000L else null,
        actualUsdcAmount = actualUsdcAmount,
        actualFiatAmount = actualFiatAmount,
        placedTxHash = "0xtx2",
        placedAtBlockNumber = 16L,
        source = OrderSnapshot.Source.Subgraph,
    )

    private class ScriptedOrderReadSource : OrderReadSource {
        private val queue = ArrayDeque<OrderSnapshot?>()
        fun enqueue(vararg snapshots: OrderSnapshot?) {
            snapshots.forEach { queue.addLast(it) }
        }
        override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? =
            if (queue.isNotEmpty()) queue.removeFirst() else null
    }

    private fun receiptFor(payload: JsonObject): String {
        val txParam = payload["params"]!!.toString().substringAfter('"').substringBefore('"')
        return when (txParam) {
            "0xtx2" -> placeOrderReceiptJson()
            else -> simpleSuccessReceiptJson(txParam)
        }
    }

    private fun ethCallResponse(payload: JsonObject): String {
        val params = payload["params"]!!.toString()
        return when {
            params.contains("0x36b0ec9a") ->
                """{"jsonrpc":"2.0","id":1,"result":"$getAssignableResponse"}"""
            else -> error("Unexpected eth_call (no longer poll getOrdersById on-chain): $params")
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
        val userTopic = "0x" + "0".repeat(24) + account.address.lowercaseHex.removePrefix("0x")
        val orderIdTopic = "0x" + ORDER_ID.toString(16).padStart(64, '0')
        return """
            {"jsonrpc":"2.0","id":1,"result":{
              "transactionHash":"0xtx2",
              "blockNumber":"0x10",
              "status":"0x1",
              "gasUsed":"0x5208",
              "logs":[
                {
                  "address":"${network.diamondAddress.lowercaseHex}",
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

    companion object {
        const val MNEMONIC =
            "abandon abandon abandon abandon abandon abandon " +
                "abandon abandon abandon abandon abandon about"
        const val MERCHANT_PUBKEY =
            "1b84c5567b126440995d3ed5aaba0565d71e1834604819ff9c17f5e9d5dd078f" +
                "70beaf8f588b541507fed6a642c5ab42dfdf8120a7f639de5122d47a69a8e8d1"
        const val MERCHANT_ADDRESS = "0x1111111111111111111111111111111111111111"
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

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
import xyz.justzappit.offramp.funding.OfframpFunding
import xyz.justzappit.offramp.funding.OfframpRefund
import xyz.justzappit.offramp.p2p.CircleRouter
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.Usdc6
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
                // Synthetic but valid 32-byte hash so TxHash.fromHex parses it. The trailing
                // byte indexes the broadcast (1..N) for the receipt mock to discriminate on.
                val tag = rawTxLog.size.toString(16).padStart(2, '0')
                """{"jsonrpc":"2.0","id":1,"result":"0x${"00".repeat(31)}$tag"}"""
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
        submitter = signer,
        accountAddress = account.address,
        network = network,
        subgraph = subgraph,
        orderReader = orderReader,
        funding = OfframpFunding { _, _ -> },
        refund = OfframpRefund { _, _ -> null },
        router = CircleRouter(random = Random(0), epsilon = 0.0),
        pollIntervalMs = 0,
        stalledAfterMs = 50,
        clockMs = ::nextTick,
    )

    // Monotonic per-call counter so tests don't depend on wall-clock advancing under runTest's
    // virtual scheduler. With stalledAfterMs=50 and a per-call increment of 20, the third
    // observation flips stalled→true regardless of how the test runtime schedules suspensions.
    private var tickCounter = 0L
    private fun nextTick(): Long {
        tickCounter += 20
        return tickCounter
    }

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
                actualFiatAmount = Usdc6.ofMicros(445_000_000),
                actualUsdcAmount = Usdc6.ofMicros(5_062_500),
                completedAtEpochSeconds = 1_779_999_999L,
            ),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = Usdc6.ofMicros(5_000_000),
                currency = CurrencyCode.Inr,
            ),
        ).toList()

        assertIs<OfframpStatus.Idle>(statuses.first())
        val completed = statuses.last() as OfframpStatus.Completed
        assertEquals(ORDER_ID, completed.orderId)
        assertEquals(Usdc6.ofMicros(445_000_000), completed.actualFiatAmount)
        assertEquals(Usdc6.ofMicros(5_062_500), completed.actualUsdcAmount)
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
                usdcAmount = Usdc6.ofMicros(5_000_000),
            ),
        ).toList()
        val last = assertIs<OfframpStatus.Failed>(statuses.last())
        assertEquals(null, last.orderId)
        assertEquals(OfframpStep.SELECTING_CIRCLE, last.step)
    }

    @Test
    fun `transient RPC errors during polling do not fail the flow`() = runTest {
        // Regression for: a single bad poll mid-flight used to throw out of orderReader.fetchOrder
        // and bail the orchestrator into Failed, orphaning escrowed USDC. After the FallbackOrderReader
        // total-fix + orchestrator catch wrap, transient failures collapse to null and polling continues.
        orderReader.enqueue(null) // primary observer "transiently fails"; reader returns null
        orderReader.enqueueThrow(RuntimeException("kapow")) // even an outright throw is absorbed
        orderReader.enqueue(snapshot(status = OrderStatus.ACCEPTED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS))
        orderReader.enqueue(
            snapshot(status = OrderStatus.COMPLETED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = Usdc6.ofMicros(5_000_000),
            ),
        ).toList()
        assertIs<OfframpStatus.Completed>(statuses.last())
    }

    @Test
    fun `WaitingForMerchantAcceptance flips stalled=true after stalledAfterMs of polling`() = runTest {
        // No accepted snapshot enqueued for a while → orchestrator loops returning null. Once we've
        // been polling longer than stalledAfterMs (50ms in tests, default 5min), the emitted
        // WaitingForMerchantAcceptance should carry stalled=true. We then drop ACCEPTED + COMPLETED
        // in to terminate the flow cleanly.
        repeat(5) { orderReader.enqueue(null) }
        orderReader.enqueue(snapshot(status = OrderStatus.ACCEPTED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS))
        orderReader.enqueue(
            snapshot(status = OrderStatus.COMPLETED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = Usdc6.ofMicros(5_000_000),
            ),
        ).toList()
        val anyStalledAcceptance = statuses
            .filterIsInstance<OfframpStatus.WaitingForMerchantAcceptance>()
            .any { it.stalled }
        assertTrue(anyStalledAcceptance, "expected at least one WaitingForMerchantAcceptance.stalled=true emission")
    }

    @Test
    fun `cancelled order during acceptance polling emits Cancelled terminal (not Failed)`() = runTest {
        orderReader.enqueue(
            snapshot(status = OrderStatus.CANCELLED, pubkey = "", merchant = null),
        )

        val statuses = orchestrator.run(
            OfframpRequest(
                recipientUpi = "merchant@upi",
                usdcAmount = Usdc6.ofMicros(5_000_000),
            ),
        ).toList()
        val last = assertIs<OfframpStatus.Cancelled>(statuses.last())
        assertEquals(ORDER_ID, last.orderId)
        assertEquals(1_779_500_000L, last.cancelledAtEpochSeconds)
        // Refunded amount falls back to the placed usdcAmount when subgraph's actualUsdcAmount
        // is null (it only populates on COMPLETED).
        assertEquals(Usdc6.ofMicros(5_000_000), last.refundedUsdcAmount)
    }

    @Test
    fun `resume does not re-send setSellOrderUpi when the order has advanced past ACCEPTED`() = runTest {
        // Process died after the setSellOrderUpi tx landed but before its hash was checkpointed, so
        // the checkpoint has no setUpiTxHash. On resume the order is already PAID; re-sending would
        // revert with UpiAlreadySent. The orchestrator must skip straight to completion polling.
        orderReader.enqueue(snapshot(status = OrderStatus.PAID, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS))
        orderReader.enqueue(snapshot(status = OrderStatus.COMPLETED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS))

        val statuses = orchestrator.resume(resumeCheckpoint(setUpiTxHash = null)).toList()

        assertIs<OfframpStatus.Completed>(statuses.last())
        assertEquals(0, rawTxLog.size, "resume must not broadcast setSellOrderUpi when UPI is already on-chain")
        assertTrue(
            statuses.none { it is OfframpStatus.SendingEncryptedUpi },
            "no SendingEncryptedUpi should be emitted when the UPI tx is skipped",
        )
    }

    @Test
    fun `resume does not re-send setSellOrderUpi when encryptedUserUpi is already populated`() = runTest {
        // Same race, detected via the on-chain field rather than the status: the order is still
        // ACCEPTED but encUpi is already set, so the tx landed and we must not re-broadcast.
        orderReader.enqueue(
            snapshot(
                status = OrderStatus.ACCEPTED,
                pubkey = MERCHANT_PUBKEY,
                merchant = MERCHANT_ADDRESS,
                encryptedUserUpi = "0xdeadbeef",
            ),
        )
        orderReader.enqueue(snapshot(status = OrderStatus.COMPLETED, pubkey = MERCHANT_PUBKEY, merchant = MERCHANT_ADDRESS))

        val statuses = orchestrator.resume(resumeCheckpoint(setUpiTxHash = null)).toList()

        assertIs<OfframpStatus.Completed>(statuses.last())
        assertEquals(0, rawTxLog.size, "resume must not broadcast setSellOrderUpi when encUpi is already on-chain")
    }

    private fun resumeCheckpoint(setUpiTxHash: xyz.justzappit.evm.types.TxHash?) = OfframpCheckpoint(
        orderId = ORDER_ID.toString(),
        currentStep = OfframpStep.WAITING_FOR_ACCEPTANCE,
        placeOrderTxHash = xyz.justzappit.evm.types.TxHash.fromHex("0x" + "02".padStart(64, '0')),
        setUpiTxHash = setUpiTxHash,
        recipientUpi = "merchant@upi",
        usdcAmountMicroDecimal = "5000000",
        currency = CurrencyCode.Inr,
        createdAtMillis = 0,
    )

    private fun snapshot(
        status: OrderStatus,
        pubkey: String,
        merchant: String?,
        actualUsdcAmount: Usdc6? = null,
        actualFiatAmount: Usdc6? = null,
        completedAtEpochSeconds: Long? = null,
        encryptedUserUpi: String = "",
    ) = OrderSnapshot(
        orderId = ORDER_ID,
        status = status,
        orderType = OrderType.PAY,
        circleId = BigInteger.ONE,
        userAddress = account.address,
        usdcAmount = Usdc6.ofMicros(5_000_000),
        fiatAmount = Usdc6.ofMicros(445_000_000),
        currencyHex = "0x494e520000000000000000000000000000000000000000000000000000000000",
        acceptedMerchantAddress = merchant?.let { Address.parse(it) },
        merchantPubKey = pubkey,
        encryptedUserUpi = encryptedUserUpi,
        encryptedMerchantUpi = "",
        placedAtEpochSeconds = 1_779_000_000L,
        acceptedAtEpochSeconds = if (status.onChain >= OrderStatus.ACCEPTED.onChain) 1_779_500_000L else null,
        paidAtEpochSeconds = null,
        completedAtEpochSeconds = completedAtEpochSeconds,
        cancelledAtEpochSeconds = if (status == OrderStatus.CANCELLED) 1_779_500_000L else null,
        actualUsdcAmount = actualUsdcAmount,
        actualFiatAmount = actualFiatAmount,
        placedTxHash = xyz.justzappit.evm.types.TxHash.fromHex(
            "0x" + "02".padStart(64, '0'),
        ),
        placedAtBlockNumber = 16L,
        source = OrderSnapshot.Source.Subgraph,
    )

    private class ScriptedOrderReadSource : OrderReadSource {
        // Items are either an OrderSnapshot, null, or a throwable to raise. ArrayDeque<Any?> with
        // throwable sentinel keeps the test ergonomic without a separate field.
        private val queue = ArrayDeque<Any?>()

        fun enqueue(vararg snapshots: OrderSnapshot?) {
            snapshots.forEach { queue.addLast(it) }
        }

        fun enqueueThrow(error: Throwable) {
            queue.addLast(ThrowSentinel(error))
        }

        override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? {
            if (queue.isEmpty()) return null
            return when (val head = queue.removeFirst()) {
                is ThrowSentinel -> throw head.error
                is OrderSnapshot -> head
                null -> null
                else -> error("unexpected scripted item: $head")
            }
        }

        private class ThrowSentinel(val error: Throwable)
    }

    private fun receiptFor(payload: JsonObject): String {
        val txParam = payload["params"]!!.toString().substringAfter('"').substringBefore('"')
        // The mock encodes the broadcast index into the trailing byte of the synthetic hash; the
        // second broadcast (placeOrder) needs to return the receipt with an OrderPlaced log.
        return when (txParam) {
            PLACE_ORDER_TX_HASH -> placeOrderReceiptJson()
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
              "transactionHash":"$PLACE_ORDER_TX_HASH",
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
                  "transactionHash":"$PLACE_ORDER_TX_HASH",
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

        // Synthetic 32-byte hash whose trailing byte (0x02) matches the second mocked broadcast.
        private const val PLACE_ORDER_TX_HASH = "0x" + "0000000000000000000000000000000000000000000000000000000000000002"

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

package xyz.justzappit.evm.rpc

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaseRpcClientTest {
    private val handledRequests = mutableListOf<JsonObject>()
    private var nextResponse: String = ""

    private val client = HttpClient(
        MockEngine { request ->
            val bodyBytes = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes()
            handledRequests += Json.parseToJsonElement(bodyBytes.decodeToString()) as JsonObject
            respond(
                content = nextResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) { json() }
    }

    private val rpc = BaseRpcClient(client, "http://mock/rpc")

    @AfterTest
    fun shutdown() {
        client.close()
    }

    @Test
    fun `ethChainId decodes hex result to Long`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":"0x14a34"}"""
        val chainId = rpc.ethChainId()
        assertEquals(84_532L, chainId)
        assertEquals("eth_chainId", handledRequests.last()["method"]!!.jsonPrimitive.content)
    }

    @Test
    fun `ethGasPrice decodes hex to BigInteger`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":"0x3b9aca00"}"""
        assertEquals(BigInteger("1000000000"), rpc.ethGasPrice())
    }

    @Test
    fun `ethGetTransactionCount sends address and tag`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":"0x2a"}"""
        val nonce = rpc.ethGetTransactionCount("0xabc", blockTag = "latest")
        assertEquals(BigInteger.valueOf(42), nonce)
        val params = handledRequests.last()["params"]!!.toString()
        assertTrue(params.contains("0xabc"))
        assertTrue(params.contains("latest"))
    }

    @Test
    fun `ethCall hex-encodes data with 0x prefix and decodes returned bytes`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":"0xdeadbeef"}"""
        val result = rpc.ethCall("0xto", byteArrayOf(0x01, 0x02))
        assertEquals("deadbeef", result.toHex())
        val sentParams = handledRequests.last()["params"]!!.toString()
        assertTrue(sentParams.contains("\"0x0102\""), "expected hex-encoded data in params, got $sentParams")
    }

    @Test
    fun `ethEstimateGas sends from to value data tuple`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":"0x5208"}"""
        val gas = rpc.ethEstimateGas(from = "0xfrom", to = "0xto", value = BigInteger.valueOf(1_000))
        assertEquals(BigInteger.valueOf(21_000), gas)
    }

    @Test
    fun `ethSendRawTransaction prepends 0x when missing and returns tx hash`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":"0xtxhash"}"""
        assertEquals("0xtxhash", rpc.ethSendRawTransaction("aabbcc"))
        val params = handledRequests.last()["params"]!!.toString()
        assertTrue(params.contains("0xaabbcc"))
    }

    @Test
    fun `ethGetTransactionReceipt returns null when result is null`() = runTest {
        nextResponse = """{"jsonrpc":"2.0","id":1,"result":null}"""
        assertNull(rpc.ethGetTransactionReceipt("0xtx"))
    }

    @Test
    fun `ethGetTransactionReceipt parses success`() = runTest {
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"result":{
              "transactionHash":"0xtx",
              "blockNumber":"0x10",
              "status":"0x1",
              "gasUsed":"0x5208",
              "effectiveGasPrice":"0x3b9aca00",
              "logs":[]
            }}
        """.trimIndent()
        val receipt = rpc.ethGetTransactionReceipt("0xtx")!!
        assertEquals("0xtx", receipt.transactionHash)
        assertTrue(receipt.success)
        assertEquals("0x5208", receipt.gasUsed)
    }

    @Test
    fun `ethGetBlockByNumber returns baseFeePerGas when present`() = runTest {
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"result":{
              "number":"0x100",
              "timestamp":"0x68000000",
              "baseFeePerGas":"0x1"
            }}
        """.trimIndent()
        val block = rpc.ethGetBlockByNumber()
        assertEquals("0x100", block.number)
        assertEquals("0x1", block.baseFeePerGas)
    }

    @Test
    fun `code 3 with no data is classified as ExecutionReverted`() = runTest {
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"error":{"code":3,"message":"execution reverted"}}
        """.trimIndent()
        val ex = assertFailsWith<RpcException.ExecutionReverted> { rpc.ethGasPrice() }
        assertEquals("eth_gasPrice", ex.method)
        assertNull(ex.selector)
        assertNull(ex.solidityErrorString)
    }

    @Test
    fun `vendor -32000 plus reverted message is classified as ExecutionReverted`() = runTest {
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"execution reverted: 0x91da284f"}}
        """.trimIndent()
        val ex = assertFailsWith<RpcException.ExecutionReverted> { rpc.ethGasPrice() }
        assertEquals("eth_gasPrice", ex.method)
        // No data field — selector cannot be recovered from the message string at this layer.
        assertNull(ex.selector)
    }

    @Test
    fun `ExecutionReverted carries selector and decoded Error(string) when data is present`() = runTest {
        val payload =
            "0x08c379a0" +
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000005" +
                "68656c6c6f000000000000000000000000000000000000000000000000000000"
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"error":{"code":3,"message":"execution reverted","data":"$payload"}}
        """.trimIndent()
        val ex = assertFailsWith<RpcException.ExecutionReverted> { rpc.ethGasPrice() }
        assertEquals("hello", ex.solidityErrorString)
        assertEquals("0x08c379a0", ex.selector?.hex)
    }

    @Test
    fun `unknown JSON-RPC error code maps to RpcException Unknown`() = runTest {
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"error":{"code":-32700,"message":"Parse error"}}
        """.trimIndent()
        val ex = assertFailsWith<RpcException.Unknown> { rpc.ethGasPrice() }
        assertEquals("eth_gasPrice", ex.method)
        assertEquals(-32_700, ex.code)
    }

    @Test
    fun `method not found maps to RpcException MethodNotFound`() = runTest {
        nextResponse = """
            {"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"Method not found"}}
        """.trimIndent()
        val ex = assertFailsWith<RpcException.MethodNotFound> { rpc.ethGasPrice() }
        assertEquals("eth_gasPrice", ex.method)
    }
}

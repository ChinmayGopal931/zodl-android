package xyz.justzappit.evm.rpc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicLong

class BaseRpcClient(
    private val httpClient: HttpClient,
    private val rpcUrl: String,
) {
    private val nextId = AtomicLong(1)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun ethChainId(): Long =
        hexToBigInteger(rpcCall("eth_chainId", emptyJsonArray).jsonPrimitive.content).toLong()

    suspend fun ethGasPrice(): BigInteger =
        hexToBigInteger(rpcCall("eth_gasPrice", emptyJsonArray).jsonPrimitive.content)

    suspend fun ethMaxPriorityFeePerGas(): BigInteger =
        hexToBigInteger(rpcCall("eth_maxPriorityFeePerGas", emptyJsonArray).jsonPrimitive.content)

    suspend fun ethGetTransactionCount(address: String, blockTag: String = "pending"): BigInteger =
        hexToBigInteger(
            rpcCall(
                "eth_getTransactionCount",
                buildJsonArray {
                    add(address)
                    add(blockTag)
                },
            ).jsonPrimitive.content,
        )

    suspend fun ethCall(to: String, data: ByteArray, blockTag: String = "latest"): ByteArray =
        rpcCall(
            "eth_call",
            buildJsonArray {
                addJsonObject {
                    put("to", to)
                    put("data", "0x" + data.toHex())
                }
                add(blockTag)
            },
        ).jsonPrimitive.content.removePrefix("0x").let { if (it.isEmpty()) byteArrayOf() else it.hexToBytes() }

    suspend fun ethEstimateGas(
        from: String,
        to: String,
        value: BigInteger = BigInteger.ZERO,
        data: ByteArray = byteArrayOf(),
    ): BigInteger = hexToBigInteger(
        rpcCall(
            "eth_estimateGas",
            buildJsonArray {
                addJsonObject {
                    put("from", from)
                    put("to", to)
                    put("value", "0x" + value.toString(16))
                    put("data", "0x" + data.toHex())
                }
            },
        ).jsonPrimitive.content,
    )

    suspend fun ethSendRawTransaction(rawTxHex: String): String =
        rpcCall(
            "eth_sendRawTransaction",
            buildJsonArray { add(if (rawTxHex.startsWith("0x")) rawTxHex else "0x$rawTxHex") },
        ).jsonPrimitive.content

    suspend fun ethGetTransactionReceipt(txHash: String): TransactionReceipt? {
        val result = rpcCall("eth_getTransactionReceipt", buildJsonArray { add(txHash) })
        if (result is JsonPrimitive && result.content == "null") return null
        if (result.toString() == "null") return null
        return json.decodeFromJsonElement(TransactionReceipt.serializer(), result)
    }

    suspend fun ethGetBlockByNumber(blockTag: String = "latest"): BlockHeader {
        val result = rpcCall(
            "eth_getBlockByNumber",
            buildJsonArray {
                add(blockTag)
                add(false)
            },
        )
        return json.decodeFromJsonElement(BlockHeader.serializer(), result)
    }

    private suspend fun rpcCall(method: String, params: JsonArray): JsonElement {
        val payload = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", nextId.getAndIncrement())
            put("method", method)
            put("params", params)
        }
        val body: JsonObject = httpClient.post(rpcUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()

        body["error"]?.let { errEl ->
            val obj = errEl.jsonObject
            val code = obj["code"]?.jsonPrimitive?.content?.toIntOrNull()
            val message = obj["message"]?.jsonPrimitive?.content
            throw RpcException(method, code, message, body.toString())
        }
        return body["result"] ?: error("RPC response missing 'result': $body")
    }

    private val emptyJsonArray = JsonArray(emptyList())
}

internal fun hexToBigInteger(hex: String): BigInteger {
    val s = hex.removePrefix("0x")
    return if (s.isEmpty()) BigInteger.ZERO else BigInteger(s, 16)
}

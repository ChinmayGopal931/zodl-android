package xyz.justzappit.evm.rpc

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import xyz.justzappit.evm.signer.UserOperationV06
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.evm.types.TxHash
import xyz.justzappit.evm.util.toHex
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ERC-4337 bundler + paymaster JSON-RPC, authenticated by the publishable `X-Client-Id` header.
 * Self-custodial: the caller signs each [UserOperationV06] locally; this client never holds a key.
 * Distinct from [BaseRpcClient] (the node RPC) — it targets `https://<chainId>.bundler.thirdweb.com/v2`
 * and speaks UserOperation methods rather than `eth_sendRawTransaction`.
 */
class BundlerClient(
    private val httpClient: HttpClient,
    private val bundlerUrl: String,
    private val clientId: String,
    private val entryPoint: Address,
    private val chainId: ChainId,
) {
    private val nextId = AtomicLong(1)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun getUserOperationGasPrice(): UserOpGasPrice =
        json.decodeFromJsonElement(
            UserOpGasPrice.serializer(),
            rpcCall("thirdweb_getUserOperationGasPrice", JsonArray(emptyList())),
        )

    suspend fun estimateUserOperationGas(op: UserOperationV06): UserOpGasEstimate =
        json.decodeFromJsonElement(
            UserOpGasEstimate.serializer(),
            rpcCall(
                "eth_estimateUserOperationGas",
                buildJsonArray {
                    add(userOpJson(op))
                    add(entryPoint.checksumHex)
                },
            ),
        )

    /**
     * Placeholder paymaster data (same shape/length as the real thing) so gas estimation accounts
     * for the paymaster's validation. ERC-7677 step before [estimateUserOperationGas].
     */
    suspend fun getPaymasterStubData(op: UserOperationV06): PaymasterResult =
        paymasterCall("pm_getPaymasterStubData", op)

    /** Asks the paymaster to sponsor [op]; the returned [PaymasterResult.paymasterAndData] goes back into the op. */
    suspend fun sponsorUserOperation(op: UserOperationV06): PaymasterResult =
        paymasterCall("pm_sponsorUserOperation", op)

    private suspend fun paymasterCall(method: String, op: UserOperationV06): PaymasterResult =
        json.decodeFromJsonElement(
            PaymasterResult.serializer(),
            rpcCall(
                method,
                buildJsonArray {
                    add(userOpJson(op))
                    add(entryPoint.checksumHex)
                    add(chainId.hex)
                },
            ),
        )

    suspend fun sendUserOperation(op: UserOperationV06): TxHash =
        TxHash.fromHex(
            rpcCall(
                "eth_sendUserOperation",
                buildJsonArray {
                    add(userOpJson(op))
                    add(entryPoint.checksumHex)
                },
            ).jsonPrimitive.content,
        )

    /** The mined transaction receipt for a userOp, or null while it is still pending. */
    suspend fun getUserOperationReceipt(userOpHash: TxHash): TransactionReceipt? {
        val result = rpcCall("eth_getUserOperationReceipt", buildJsonArray { add(userOpHash.hex) })
        // Pending → result is JSON null (or otherwise not an object); not yet mined.
        val receipt = (result as? JsonObject)?.get("receipt") ?: return null
        return json.decodeFromJsonElement(TransactionReceipt.serializer(), receipt)
    }

    private fun userOpJson(op: UserOperationV06): JsonObject = buildJsonObject {
        put("sender", op.sender.checksumHex)
        putHex("nonce", op.nonce)
        putBytes("initCode", op.initCode)
        putBytes("callData", op.callData)
        putHex("callGasLimit", op.callGasLimit)
        putHex("verificationGasLimit", op.verificationGasLimit)
        putHex("preVerificationGas", op.preVerificationGas)
        putHex("maxFeePerGas", op.maxFeePerGas)
        putHex("maxPriorityFeePerGas", op.maxPriorityFeePerGas)
        putBytes("paymasterAndData", op.paymasterAndData)
        putBytes("signature", op.signature)
    }

    private fun JsonObjectBuilder.putHex(key: String, value: BigInteger) =
        put(key, "0x" + value.toString(HEX_BASE))

    private fun JsonObjectBuilder.putBytes(key: String, value: ByteArray) =
        put(key, "0x" + value.toHex())

    private suspend fun rpcCall(method: String, params: JsonArray): JsonElement {
        val payload = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", nextId.getAndIncrement())
            put("method", method)
            put("params", params)
        }
        // TEMP-DEBUG: ktor's BODY-level logging drops thirdweb's HTTP/2 bodies (same root cause as
        // the bodyAsText() workaround below). Log payload + response body inline so we can diagnose
        // bundler 500s. Keeping it on until the cancel/recovery flow stops surprising us.
        println("BundlerClient REQ $method: $payload")
        val response = try {
            httpClient.post(bundlerUrl) {
                contentType(ContentType.Application.Json)
                header(CLIENT_ID_HEADER, clientId)
                setBody(payload)
            }
        } catch (e: IOException) {
            throw RpcException.TransportError(method, e)
        }
        if (response.status == HttpStatusCode.TooManyRequests) {
            throw RpcException.RateLimited(method, retryAfterMillis = null)
        }
        // Parse from text rather than relying on ContentNegotiation: thirdweb's bundler serves over
        // HTTP/2 and ktor/OkHttp doesn't always negotiate those responses into a JsonObject, failing
        // with "expected JsonObject but was SourceByteReadChannel". Reading the text is content-type-
        // agnostic and also lets us surface the bundler's own error bodies.
        val text = response.bodyAsText()
        println("BundlerClient RES $method status=${response.status.value} body=$text")
        val element = json.parseToJsonElement(text)
        // JSON-RPC replies are envelopes ({jsonrpc,id,result|error}), but the bundler sometimes
        // returns a bare result value (e.g. eth_sendUserOperation's userOpHash). A non-object reply
        // can only be a result — errors are always objects — so pass it straight through.
        val body = element as? JsonObject ?: return element
        body["error"]?.let { errEl ->
            val err = errEl.jsonObject
            throw RpcException.Unknown(
                method = method,
                code = err["code"]?.jsonPrimitive?.content?.toIntOrNull(),
                raw = body.toString(),
                errorMessage = err["message"]?.jsonPrimitive?.content,
            )
        }
        return body["result"] ?: error("Bundler response missing 'result': $body")
    }

    companion object {
        private const val HEX_BASE = 16
        private const val CLIENT_ID_HEADER = "X-Client-Id"

        /** thirdweb's bundler URL for [chainId]; the same Client ID authenticates every chain. */
        fun urlFor(chainId: ChainId): String = "https://${chainId.value}.bundler.thirdweb.com/v2"
    }
}

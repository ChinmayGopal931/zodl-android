package xyz.justzappit.offramp.p2p

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import xyz.justzappit.evm.types.Address
import java.math.BigInteger

class SubgraphOrderReader(
    private val subgraph: SubgraphClient,
) : OrderReadSource {

    override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? {
        val raw = subgraph.rawOrderById(orderId.toString()) ?: return null
        return parse(raw)
    }

    private fun parse(node: JsonObject): OrderSnapshot {
        val orderIdValue = node.requireString("orderId").toBigInteger()
        val typeValue = node.requireString("type").toInt()
        val statusValue = node.requireString("status").toInt()
        return OrderSnapshot(
            orderId = orderIdValue,
            status = OrderStatus.fromOnChain(statusValue),
            orderType = orderTypeFromOnChain(typeValue),
            circleId = node.requireString("circleId").toBigInteger(),
            userAddress = parseNullableAddress(node.requireString("userAddress"))
                ?: Address.ZERO,
            usdcAmount = node.requireString("usdcAmount").toBigInteger(),
            fiatAmount = node.requireString("fiatAmount").toBigInteger(),
            currencyHex = node.requireString("currency"),
            acceptedMerchantAddress = parseNullableAddress(node.optionalString("acceptedMerchantAddress")),
            merchantPubKey = node.optionalString("pubkey").orEmpty(),
            encryptedUserUpi = node.optionalString("encUpi").orEmpty(),
            encryptedMerchantUpi = node.optionalString("encMerchantUpi").orEmpty(),
            placedAtEpochSeconds = parseEpochSecondsOrNull(node.optionalString("placedAt")),
            acceptedAtEpochSeconds = parseEpochSecondsOrNull(node.optionalString("acceptedAt")),
            paidAtEpochSeconds = parseEpochSecondsOrNull(node.optionalString("paidAt")),
            completedAtEpochSeconds = parseEpochSecondsOrNull(node.optionalString("completedAt")),
            cancelledAtEpochSeconds = parseEpochSecondsOrNull(node.optionalString("cancelledAt")),
            actualUsdcAmount = node.optionalString("actualUsdcAmount")?.takeIf { it != "0" }?.toBigInteger(),
            actualFiatAmount = node.optionalString("actualFiatAmount")?.takeIf { it != "0" }?.toBigInteger(),
            placedTxHash = node.optionalString("transactionHash")?.takeIf { it.isNotBlank() },
            placedAtBlockNumber = node.optionalString("blockNumber")?.toLongOrNull(),
            source = OrderSnapshot.Source.Subgraph,
        )
    }

    private fun orderTypeFromOnChain(onChain: Int): OrderType =
        OrderType.entries.firstOrNull { it.onChain == onChain }
            ?: error("Unknown OrderType from subgraph: $onChain")

    private fun JsonObject.requireString(key: String): String =
        this[key]?.jsonPrimitive?.content
            ?: error("subgraph order response missing required field '$key'")

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}

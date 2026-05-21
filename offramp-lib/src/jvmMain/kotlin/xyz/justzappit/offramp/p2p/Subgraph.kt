package xyz.justzappit.offramp.p2p

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
data class CircleForRouting(
    val circleId: String,
    val currency: String,
    val metrics: CircleMetrics,
)

@Serializable
data class CircleMetrics(
    // Subgraph emits BigDecimal-as-string; parsed into a Double via [score].
    val circleScore: String,
    val circleStatus: String,
    val scoreState: CircleScoreState,
) {
    val score: Double get() = circleScore.toDoubleOrNull() ?: 0.0
}

@Serializable
data class CircleScoreState(
    val activeMerchantsCount: String,
)

class SubgraphClient(
    private val httpClient: HttpClient,
    private val subgraphUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun circlesForRouting(currencyBytes32Hex: String): List<CircleForRouting> {
        val data = query(
            query = CIRCLES_FOR_ROUTING_QUERY,
            variables = buildJsonObject { put("currency", currencyBytes32Hex) },
        )
        val circles = data["circles"]?.jsonArray ?: error("subgraph response missing 'circles'")
        return circles
            .map { json.decodeFromJsonElement(CircleForRouting.serializer(), it) }
            .filter { (it.metrics.scoreState.activeMerchantsCount.toIntOrNull() ?: 0) > 0 }
    }

    suspend fun rawOrderById(orderId: String): JsonObject? {
        val data = query(
            query = ORDER_BY_ID_QUERY,
            variables = buildJsonObject { put("orderId", orderId) },
        )
        val orders = data["orders_collection"]?.jsonArray ?: return null
        return orders.firstOrNull()?.jsonObject
    }

    private suspend fun query(query: String, variables: JsonElement): JsonObject {
        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }
        val response: JsonObject = httpClient.post(subgraphUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()

        response["errors"]?.let { errs -> error("subgraph errors: $errs") }
        return response["data"]?.jsonObject ?: error("subgraph response missing 'data': $response")
    }

    companion object {
        const val ORDER_BY_ID_QUERY = """
            query OrderById(${'$'}orderId: BigInt!) {
              orders_collection(where: { orderId: ${'$'}orderId }) {
                orderId
                type
                status
                circleId
                userAddress
                usdcAmount
                fiatAmount
                currency
                placedAt
                acceptedAt
                paidAt
                completedAt
                cancelledAt
                acceptedMerchantAddress
                pubkey
                encUpi
                encMerchantUpi
                actualUsdcAmount
                actualFiatAmount
                blockNumber
                blockTimestamp
                transactionHash
              }
            }
        """


        // Mirrors p2pdotme-sdk/src/orders/internal/routing/subgraph/queries.ts
        const val CIRCLES_FOR_ROUTING_QUERY = """
            query CirclesForRouting(${'$'}currency: Bytes!) {
              circles(
                first: 1000
                where: {
                  currency: ${'$'}currency
                  metrics_: {
                    circleStatus_in: ["active", "bootstrap", "paused"]
                  }
                }
              ) {
                circleId
                currency
                metrics {
                  circleScore
                  circleStatus
                  scoreState {
                    activeMerchantsCount
                  }
                }
              }
            }
        """
    }
}

package xyz.justzappit.offramp.p2p

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
        val payload = buildJsonObject {
            put("query", CIRCLES_FOR_ROUTING_QUERY)
            put(
                "variables",
                buildJsonObject {
                    put("currency", currencyBytes32Hex)
                },
            )
        }
        val response: JsonObject = httpClient.post(subgraphUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()

        response["errors"]?.let { errs -> error("subgraph errors: $errs") }
        val data = response["data"]?.jsonObject ?: error("subgraph response missing 'data': $response")
        val circles = data["circles"]?.jsonArray ?: error("subgraph response missing 'circles'")
        return circles
            .map { json.decodeFromJsonElement(CircleForRouting.serializer(), it) }
            .filter { (it.metrics.scoreState.activeMerchantsCount.toIntOrNull() ?: 0) > 0 }
    }

    companion object {
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

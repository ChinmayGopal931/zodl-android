package xyz.justzappit.offramp.p2p

import kotlinx.coroutines.CancellationException
import java.math.BigInteger

fun interface OrderReadLogger {
    fun warn(message: String, cause: Throwable?)
}

class FallbackOrderReader(
    private val primary: OrderReadSource,
    private val fallback: OrderReadSource,
    private val logger: OrderReadLogger? = null,
) : OrderReadSource {

    override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? {
        val primaryResult = runPrimary(orderId)
        if (primaryResult != null) return primaryResult
        return fallback.fetchOrder(orderId)
    }

    private suspend fun runPrimary(orderId: BigInteger): OrderSnapshot? {
        return try {
            primary.fetchOrder(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger?.warn("Primary order source failed for orderId=$orderId; falling back", e)
            null
        }
    }
}

package xyz.justzappit.offramp.p2p

import kotlinx.coroutines.CancellationException
import java.math.BigInteger

fun interface OrderReadLogger {
    fun warn(message: String, cause: Throwable?)
}

/**
 * Composite [OrderReadSource] that tries [primary] first and falls through to [fallback]
 * on exception. Cancellation is propagated. If both throw, the second exception is
 * re-thrown so callers see a real error rather than a spurious null.
 *
 * Used to make the subgraph the happy path while keeping the on-chain Diamond read as
 * a hot standby for indexer outages.
 */
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

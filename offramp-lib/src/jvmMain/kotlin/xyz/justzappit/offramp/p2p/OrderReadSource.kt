package xyz.justzappit.offramp.p2p

import java.math.BigInteger

/**
 * Polled read of a single order's current state. Implementations may hit a GraphQL
 * subgraph, the on-chain Diamond, a cache, etc. Returns `null` when the order is not
 * yet visible to that source (subgraph indexer lag, etc.) — callers should retry.
 *
 * Implementations must throw on transport / parse failures so a composite
 * [FallbackOrderReader] can route around them.
 */
interface OrderReadSource {
    suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot?
}

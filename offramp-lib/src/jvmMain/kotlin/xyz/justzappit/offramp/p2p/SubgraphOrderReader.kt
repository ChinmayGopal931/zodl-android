package xyz.justzappit.offramp.p2p

import java.math.BigInteger

class SubgraphOrderReader(
    private val subgraph: SubgraphClient,
) : OrderReadSource {

    override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? {
        val raw = subgraph.rawOrderById(orderId.toString()) ?: return null
        return SubgraphOrderParser.parse(raw)
    }
}

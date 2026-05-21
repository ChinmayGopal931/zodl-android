package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.offramp.config.P2pNetworkConfig
import java.math.BigInteger

/**
 * Fallback [OrderReadSource]. One `eth_call(getOrdersById)` to the Diamond followed by an
 * ABI decode of the Order tuple. Misses the per-event timestamps and `actualFiatAmount`
 * that the subgraph carries — those fields land as `null` on the returned snapshot.
 *
 * Used when the subgraph is unavailable or doesn't yet have the order indexed.
 */
class OnChainOrderReader(
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
) : OrderReadSource {

    override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? {
        val returnData = rpc.ethCall(
            to = network.diamondAddress,
            data = DiamondCalls.getOrdersByIdCalldata(orderId),
        )
        if (returnData.isEmpty()) return null
        return OrderReader.decodeOrderSnapshot(returnData, orderId)
    }
}

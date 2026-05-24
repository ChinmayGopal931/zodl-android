package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.offramp.config.P2pNetworkConfig
import java.math.BigInteger

class OnChainOrderReader(
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
) : OrderReadSource {
    override suspend fun fetchOrder(orderId: BigInteger): OrderSnapshot? {
        val returnData =
            rpc.ethCall(
                to = network.diamondAddress,
                data = DiamondCalls.getOrdersByIdCalldata(orderId),
            )
        if (returnData.isEmpty()) return null
        return OrderReader.decodeOrderSnapshot(returnData, orderId)
    }
}

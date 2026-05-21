package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.spackle.Twig
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.OrderFeeDetails
import xyz.justzappit.offramp.p2p.OrderFeeDetailsDecoder
import java.math.BigInteger

/**
 * Fetches the post-execution fee breakdown for an order via the Diamond's
 * `getAdditionalOrderDetails(uint256)`. Returns null on any error so the caller can fall back
 * to subgraph-derived data — this read is purely a UX enrichment (it surfaces `fixedFeePaid`,
 * which the subgraph never indexes).
 *
 * Safe to call repeatedly as the order progresses: timestamps and amounts populate
 * incrementally on-chain (acceptedTimestamp on ACCEPTED, paidTimestamp on PAID, actuals on
 * COMPLETED) and the decoder treats unwritten fields as zero/null.
 */
internal class GetOrderFeeDetailsUseCase(
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
) {
    suspend operator fun invoke(orderId: BigInteger): OrderFeeDetails? =
        runCatching {
            val raw = rpc.ethCall(
                to = network.diamondAddress,
                data = DiamondCalls.getAdditionalOrderDetailsCalldata(orderId),
            )
            OrderFeeDetailsDecoder.decode(raw)
        }.onFailure {
            Twig.warn(it) { "GetOrderFeeDetailsUseCase: getAdditionalOrderDetails($orderId) failed" }
        }.getOrNull()
}

package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.spackle.Twig
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.DiamondCalls
import xyz.justzappit.offramp.p2p.PriceConfigDecoder
import java.math.BigDecimal

class GetUpiOfframpRateUseCase(
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
) {
    suspend operator fun invoke(currency: String): BigDecimal? =
        runCatching {
            val raw = rpc.ethCall(
                to = network.diamondAddress,
                data = DiamondCalls.getPriceConfigCalldata(currency),
            )
            PriceConfigDecoder.decode(raw).sellPriceAsRate()
        }.onFailure {
            Twig.warn(it) { "GetUpiOfframpRateUseCase: getPriceConfig($currency) failed" }
        }.getOrNull()
}

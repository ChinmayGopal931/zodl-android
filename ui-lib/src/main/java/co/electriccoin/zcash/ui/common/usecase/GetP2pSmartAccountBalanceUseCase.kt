package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.spackle.Twig
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.account.SmartOfframpAccountProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.p2p.Erc20Calls
import xyz.justzappit.offramp.p2p.Usdc6
import java.math.BigInteger

/**
 * Reads the USDC balance held by the offramp smart account. Returns null on any failure (RPC
 * down, smart-account factory unreachable) so the UI can render a "—" rather than crash.
 */
internal class GetP2pSmartAccountBalanceUseCase(
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
    private val accountProvider: SmartOfframpAccountProvider,
) {
    suspend operator fun invoke(): Result? = runCatching {
        val address = accountProvider.resolve().address
        val raw = rpc.ethCall(to = network.usdcAddress, data = Erc20Calls.balanceOfCalldata(address))
        val micros = if (raw.isEmpty()) BigInteger.ZERO else BigInteger(1, raw)
        Result(address = address, balance = Usdc6(micros))
    }.onFailure {
        Twig.warn(it) { "GetP2pSmartAccountBalanceUseCase failed" }
    }.getOrNull()

    data class Result(val address: Address, val balance: Usdc6)
}

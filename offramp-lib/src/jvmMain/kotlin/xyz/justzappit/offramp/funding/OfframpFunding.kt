package xyz.justzappit.offramp.funding

import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.p2p.Erc20Calls
import java.math.BigInteger

/**
 * Makes the smart account hold the USDC an order needs. Called by the orchestrator **after** the
 * circle-eligibility gate and **before** approve/placeOrder, so we never fund (bridge) into a market
 * with no merchant. Network-toggled in DI: [PreFundedOfframpFunding] on testnet (expects manual
 * funding), the NEAR-bridge implementation on mainnet.
 */
fun interface OfframpFunding {
    suspend fun ensureFunded(account: Address, request: OfframpRequest)
}

/**
 * Testnet/dev funding: no bridge (NEAR has no testnet route). Verifies the account already holds the
 * order amount and fails fast with an actionable message otherwise.
 */
class PreFundedOfframpFunding(
    private val rpc: BaseRpcClient,
    private val usdc: Address,
) : OfframpFunding {
    override suspend fun ensureFunded(account: Address, request: OfframpRequest) {
        val balance = balanceOf(account)
        check(balance >= request.usdcAmount.micros) {
            "Smart account ${account.checksumHex} holds $balance USDC (micros), needs " +
                "${request.usdcAmount.micros}. Fund it directly — no bridge on testnet."
        }
    }

    private suspend fun balanceOf(account: Address): BigInteger {
        val ret = rpc.ethCall(to = usdc, data = Erc20Calls.balanceOfCalldata(account))
        return if (ret.isEmpty()) BigInteger.ZERO else BigInteger(1, ret)
    }
}

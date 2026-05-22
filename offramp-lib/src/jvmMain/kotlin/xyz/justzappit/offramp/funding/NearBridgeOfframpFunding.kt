package xyz.justzappit.offramp.funding

import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.orchestrator.OfframpRequest
import xyz.justzappit.offramp.p2p.Erc20Calls
import java.math.BigInteger

/**
 * Mainnet funding via NEAR Intents 1-Click (doc §3.5): bridge ZEC → USDC into the smart account.
 *
 * Idempotent and resume-safe: it first checks the on-chain balance and only bridges the shortfall,
 * so a process death mid-flow that re-enters `run()` won't double-bridge. By the time this runs the
 * orchestrator has already confirmed an assignable merchant (the circle-eligibility gate), so we
 * never bridge into a market with no route.
 *
 * The NEAR leg itself is the remaining mainnet integration point — see [bridgeZecToUsdc].
 */
class NearBridgeOfframpFunding(
    private val rpc: BaseRpcClient,
    private val usdc: Address,
) : OfframpFunding {
    override suspend fun ensureFunded(account: Address, request: OfframpRequest) {
        if (balanceOf(account) >= request.usdcAmount.micros) return
        bridgeZecToUsdc(account, request)
        check(balanceOf(account) >= request.usdcAmount.micros) {
            "NEAR bridge did not deliver enough USDC to ${account.checksumHex} for the order."
        }
    }

    private suspend fun bridgeZecToUsdc(account: Address, request: OfframpRequest) {
        // TODO(mainnet): NEAR 1-Click quote (originAsset=ZEC, destinationAsset=USDC on Base,
        //  recipient=account, refundTo=account), then drive the ZEC deposit and poll until the USDC
        //  lands at [account]. Reuse NearSwapDataSourceImpl. Until then mainnet offramp is gated off
        //  (the account provider's mainnet kill-switch), so this path is unreachable in shipped builds.
        error("NEAR bridge-in for mainnet offramp is not yet implemented")
    }

    private suspend fun balanceOf(account: Address): BigInteger {
        val ret = rpc.ethCall(to = usdc, data = Erc20Calls.balanceOfCalldata(account))
        return if (ret.isEmpty()) BigInteger.ZERO else BigInteger(1, ret)
    }
}

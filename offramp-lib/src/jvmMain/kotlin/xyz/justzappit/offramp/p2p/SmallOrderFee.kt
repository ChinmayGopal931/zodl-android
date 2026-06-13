package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiDecoder
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address

/**
 * Reads `getSmallOrderFixedFeePay(currency)` — the fixed USDC fee the Diamond pulls as a *second*
 * `transferFrom` inside `setSellOrderUpi`, on top of the placed amount. A caller must cover
 * `placed + fee` in BOTH allowance and account balance or the contract atomic-cancels the order from
 * inside the user's own setUpi tx (looks like a merchant decline). Verified mainnet 2026-05-24.
 */
suspend fun BaseRpcClient.getSmallOrderFixedFeePay(diamondAddress: Address, currency: CurrencyCode): Usdc6 {
    val ret = ethCall(to = diamondAddress, data = DiamondCalls.getSmallOrderFixedFeePayCalldata(currency))
    return Usdc6(AbiDecoder(ret).also { it.requireWords(1) }.uint(0))
}

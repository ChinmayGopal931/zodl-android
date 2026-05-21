package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.evm.abi.Selector4
import xyz.justzappit.evm.rpc.RpcException

object KnownReverts {
    private val SELECTOR_LOOKUP: Map<Selector4, KnownRevertReason> = mapOf(
        Selector4.fromHex("0x91da284f") to KnownRevertReason.InsufficientReputation,
        Selector4.fromHex("0x5d04ff4c") to KnownRevertReason.NoMerchantLiquidity,
    )

    fun explain(reverted: RpcException.ExecutionReverted): KnownRevertReason? =
        reverted.selector?.let { SELECTOR_LOOKUP[it] }

    fun explain(selector: Selector4?): KnownRevertReason? = selector?.let { SELECTOR_LOOKUP[it] }
}

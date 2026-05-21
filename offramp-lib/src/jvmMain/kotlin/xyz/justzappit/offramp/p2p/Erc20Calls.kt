package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiAddress
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.abi.AbiUint
import xyz.justzappit.evm.types.Address

object Erc20Calls {
    fun approveCalldata(spender: Address, amount: Usdc6): ByteArray =
        AbiEncoder.encodeFunctionCall(
            "approve(address,uint256)",
            listOf(AbiAddress(spender), AbiUint(amount.micros)),
        )
}

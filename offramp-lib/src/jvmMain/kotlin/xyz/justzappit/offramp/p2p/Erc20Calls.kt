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

    fun balanceOfCalldata(owner: Address): ByteArray =
        AbiEncoder.encodeFunctionCall("balanceOf(address)", listOf(AbiAddress(owner)))

    fun transferCalldata(to: Address, amount: Usdc6): ByteArray =
        AbiEncoder.encodeFunctionCall(
            "transfer(address,uint256)",
            listOf(AbiAddress(to), AbiUint(amount.micros)),
        )
}

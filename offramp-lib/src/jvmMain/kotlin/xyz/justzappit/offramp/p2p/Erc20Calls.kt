package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiAddress
import xyz.justzappit.evm.abi.AbiEncoder
import xyz.justzappit.evm.abi.AbiUint
import xyz.justzappit.evm.types.Address
import java.math.BigInteger

object Erc20Calls {
    fun approveCalldata(spender: Address, amount: BigInteger): ByteArray =
        AbiEncoder.encodeFunctionCall(
            "approve(address,uint256)",
            listOf(AbiAddress(spender), AbiUint(amount)),
        )
}

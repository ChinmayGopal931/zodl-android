package xyz.justzappit.evm.signer

import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.types.ChainId
import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class UserOperationV06Test {
    // EntryPoint v0.6 — the address p2p.me uses and thirdweb's bundler exposes.
    private val entryPoint = Address.parse("0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789")

    @Test
    fun `userOpHash matches on-chain EntryPoint v0_6 getUserOpHash`() {
        // Golden vector pinned from the live EntryPoint v0.6 on Base Sepolia (chain 84532):
        //   cast call 0x5FF137D4... \
        //     "getUserOpHash((address,uint256,bytes,bytes,uint256,uint256,uint256,uint256,uint256,bytes,bytes))(bytes32)" \
        //     '(0x0000000000000000000000000000000000000001,1,0x,0xdeadbeef,2,3,4,5,6,0x,0x)'
        val op = UserOperationV06(
            sender = Address.parse("0x0000000000000000000000000000000000000001"),
            nonce = BigInteger.ONE,
            initCode = ByteArray(0),
            callData = "deadbeef".hexToBytes(),
            callGasLimit = BigInteger.valueOf(2),
            verificationGasLimit = BigInteger.valueOf(3),
            preVerificationGas = BigInteger.valueOf(4),
            maxFeePerGas = BigInteger.valueOf(5),
            maxPriorityFeePerGas = BigInteger.valueOf(6),
            paymasterAndData = ByteArray(0),
            signature = ByteArray(0),
        )
        assertEquals(
            "12fbc9ec3ac58304724217290201c9ab56bf4c25a16b1b1fc201876841660e1e",
            op.userOpHash(entryPoint, ChainId.BASE_SEPOLIA).toHex(),
        )
    }

    @Test
    fun `signature does not affect userOpHash`() {
        val base = UserOperationV06(
            sender = Address.parse("0x0000000000000000000000000000000000000001"),
            nonce = BigInteger.ONE,
            initCode = ByteArray(0),
            callData = "deadbeef".hexToBytes(),
            callGasLimit = BigInteger.valueOf(2),
            verificationGasLimit = BigInteger.valueOf(3),
            preVerificationGas = BigInteger.valueOf(4),
            maxFeePerGas = BigInteger.valueOf(5),
            maxPriorityFeePerGas = BigInteger.valueOf(6),
            paymasterAndData = ByteArray(0),
        )
        val signed = base.copy(signature = "aabbcc".hexToBytes())
        assertEquals(
            base.userOpHash(entryPoint, ChainId.BASE_SEPOLIA).toHex(),
            signed.userOpHash(entryPoint, ChainId.BASE_SEPOLIA).toHex(),
        )
    }
}

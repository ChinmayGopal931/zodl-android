package xyz.justzappit.offramp.orchestrator

import xyz.justzappit.evm.abi.Selector4
import xyz.justzappit.evm.abi.SolidityErrors
import xyz.justzappit.evm.rpc.RpcException
import xyz.justzappit.evm.util.hexToBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KnownRevertsTest {

    @Test
    fun `explain maps 0x91da284f to InsufficientReputation`() {
        val ex = revertedWith("0x91da284f")
        assertEquals(KnownRevertReason.InsufficientReputation, KnownReverts.explain(ex))
    }

    @Test
    fun `explain maps 0x5d04ff4c to NoMerchantLiquidity`() {
        val ex = revertedWith("0x5d04ff4c")
        assertEquals(KnownRevertReason.NoMerchantLiquidity, KnownReverts.explain(ex))
    }

    @Test
    fun `explain returns null for unknown selectors`() {
        val ex = revertedWith("0xdeadbeef")
        assertNull(KnownReverts.explain(ex))
    }

    @Test
    fun `selector-only overload also resolves`() {
        val selector = Selector4.fromHex("0x91da284f")
        assertEquals(KnownRevertReason.InsufficientReputation, KnownReverts.explain(selector))
        assertNull(KnownReverts.explain(null))
    }

    @Test
    fun `SolidityErrors decodes an Error(string) payload`() {
        // 0x08c379a0 || offset(0x20) || length(0x05) || "hello" || padding
        val payload = (
            "0x08c379a0" +
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000005" +
                "68656c6c6f000000000000000000000000000000000000000000000000000000"
            ).hexToBytes()
        assertEquals("hello", SolidityErrors.decodeErrorString(payload))
    }

    @Test
    fun `SolidityErrors returns null for non-Error payloads`() {
        assertNull(SolidityErrors.decodeErrorString("0x91da284f".hexToBytes()))
        assertNull(SolidityErrors.decodeErrorString(byteArrayOf()))
    }

    private fun revertedWith(selectorHex: String): RpcException.ExecutionReverted = RpcException.ExecutionReverted(
        method = "eth_call",
        selector = Selector4.fromHex(selectorHex),
        data = selectorHex.hexToBytes(),
        solidityErrorString = null,
        rawMessage = "execution reverted",
    )
}

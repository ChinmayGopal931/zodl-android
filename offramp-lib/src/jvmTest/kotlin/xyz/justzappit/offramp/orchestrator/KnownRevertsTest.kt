package xyz.justzappit.offramp.orchestrator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnownRevertsTest {

    @Test
    fun `extractSelector pulls the first 8-hex selector from an RPC error message`() {
        val raw = "RPC eth_sendRawTransaction failed with code=-32000: execution reverted: 0x91da284f"
        assertEquals("0x91da284f", KnownReverts.extractSelector(raw))
    }

    @Test
    fun `extractSelector is case-insensitive but normalises to lowercase`() {
        assertEquals(
            "0x91da284f",
            KnownReverts.extractSelector("reverted: 0x91DA284F"),
        )
    }

    @Test
    fun `extractSelector returns null when no 8-hex token is present`() {
        assertNull(KnownReverts.extractSelector("RPC connection refused"))
        assertNull(KnownReverts.extractSelector(""))
        assertNull(KnownReverts.extractSelector(null))
    }

    @Test
    fun `explain returns the RP message for 0x91da284f`() {
        val msg = KnownReverts.explain("0x91da284f")!!
        assertTrue(msg.contains("reputation", ignoreCase = true))
    }

    @Test
    fun `explain returns the liquidity message for 0x5d04ff4c`() {
        val msg = KnownReverts.explain("0x5d04ff4c")!!
        assertTrue(msg.contains("liquidity", ignoreCase = true) || msg.contains("merchant", ignoreCase = true))
    }

    @Test
    fun `explain returns null for unknown selectors`() {
        assertNull(KnownReverts.explain("0xdeadbeef"))
        assertNull(KnownReverts.explain(null))
    }

    @Test
    fun `decodeErrorString recovers an embedded message from an Error(string) revert`() {
        // 0x08c379a0 || offset(0x20) || length(0x05) || "hello" || padding
        val payload = "0x08c379a0" +
            "0000000000000000000000000000000000000000000000000000000000000020" +
            "0000000000000000000000000000000000000000000000000000000000000005" +
            "68656c6c6f000000000000000000000000000000000000000000000000000000"
        assertEquals("hello", KnownReverts.decodeErrorString(payload))
    }

    @Test
    fun `decodeErrorString returns null for non-Error payloads`() {
        assertNull(KnownReverts.decodeErrorString("0x91da284f"))
        assertNull(KnownReverts.decodeErrorString(null))
    }
}

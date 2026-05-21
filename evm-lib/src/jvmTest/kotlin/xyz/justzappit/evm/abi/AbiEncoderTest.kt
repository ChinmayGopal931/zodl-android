package xyz.justzappit.evm.abi

import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AbiEncoderTest {

    @Test
    fun `function selector for approve matches the well-known value`() {
        assertEquals("095ea7b3", FunctionSelector.computeHex("approve(address,uint256)"))
    }

    @Test
    fun `function selector for transfer matches the well-known value`() {
        assertEquals("a9059cbb", FunctionSelector.computeHex("transfer(address,uint256)"))
    }

    @Test
    fun `function selector for balanceOf matches the well-known value`() {
        assertEquals("70a08231", FunctionSelector.computeHex("balanceOf(address)"))
    }

    @Test
    fun `function selector for placeOrder is byte-stable`() {
        // Frozen at the canonical SDK signature. Any change here would mean the canonical
        // signature drifted (function added/removed/renamed/retyped) — re-derive after
        // cross-checking the SDK's order-flow-facet ABI.
        assertEquals(
            "1dc46885",
            FunctionSelector.computeHex(
                "placeOrder(string,uint256,address,uint8,string,string,bytes32,uint256,uint256,uint256)",
            ),
        )
    }

    @Test
    fun `function selector for setSellOrderUpi is byte-stable`() {
        assertEquals(
            "e8576b23",
            FunctionSelector.computeHex("setSellOrderUpi(uint256,string,uint256)"),
        )
    }

    @Test
    fun `uint256 encodes as 32-byte big-endian, left-padded with zeros`() {
        val encoded = AbiEncoder.encode(listOf(AbiUint(BigInteger.valueOf(1_000_000)))).toHex()
        assertEquals("00000000000000000000000000000000000000000000000000000000000f4240", encoded)
    }

    @Test
    fun `address encodes left-padded with 12 zero bytes`() {
        val addr = "0xce868398FDaDcA368EAc203222874D6888532aE2"
        val encoded = AbiEncoder.encode(listOf(AbiAddress(addr))).toHex()
        assertEquals("000000000000000000000000ce868398fdadca368eac203222874d6888532ae2", encoded)
    }

    @Test
    fun `bytes32 encodes as-is, 32 bytes`() {
        val data = ByteArray(32) { 0xab.toByte() }
        val encoded = AbiEncoder.encode(listOf(AbiBytes32(data))).toHex()
        assertEquals("ab".repeat(32), encoded)
    }

    @Test
    fun `bytes32-of-string right-pads with zeros`() {
        val encoded = AbiEncoder.encode(listOf(AbiEncoder.bytes32String("INR"))).toHex()
        assertEquals("494e520000000000000000000000000000000000000000000000000000000000", encoded)
    }

    @Test
    fun `uint8 encodes as 32-byte left-padded value`() {
        val encoded = AbiEncoder.encode(listOf(AbiUint8(2))).toHex()
        assertEquals("0000000000000000000000000000000000000000000000000000000000000002", encoded)
    }

    @Test
    fun `bool true and false encode correctly`() {
        assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000001",
            AbiEncoder.encode(listOf(AbiBool(true))).toHex(),
        )
        assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000000",
            AbiEncoder.encode(listOf(AbiBool(false))).toHex(),
        )
    }

    @Test
    fun `int256 encodes positive as left-padded, negative as twos-complement`() {
        assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000005",
            AbiEncoder.encode(listOf(AbiInt(BigInteger.valueOf(5)))).toHex(),
        )
        // -1 → 0xff..ff
        assertEquals(
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            AbiEncoder.encode(listOf(AbiInt(BigInteger.valueOf(-1)))).toHex(),
        )
    }

    @Test
    fun `approve calldata matches the hand-computed reference`() {
        // approve(0xce868398FDaDcA368EAc203222874D6888532aE2, 1_000_000)
        val calldata = AbiEncoder.encodeFunctionCall(
            "approve(address,uint256)",
            listOf(
                AbiAddress("0xce868398FDaDcA368EAc203222874D6888532aE2"),
                AbiUint(BigInteger.valueOf(1_000_000)),
            ),
        ).toHex()
        assertEquals(
            "095ea7b3" +
                "000000000000000000000000ce868398fdadca368eac203222874d6888532ae2" +
                "00000000000000000000000000000000000000000000000000000000000f4240",
            calldata,
        )
    }

    @Test
    fun `string encodes with offset, length, and right-padded data`() {
        // single dynamic arg → head is offset 0x20, tail is length + data
        val encoded = AbiEncoder.encode(listOf(AbiString("hello"))).toHex()
        val expected =
            "0000000000000000000000000000000000000000000000000000000000000020" + // offset
                "0000000000000000000000000000000000000000000000000000000000000005" + // length
                "68656c6c6f000000000000000000000000000000000000000000000000000000" // "hello" + padding
        assertEquals(expected, encoded)
    }

    @Test
    fun `string longer than 32 bytes spans multiple words with zero-padding`() {
        val s = "a".repeat(40)
        val encoded = AbiEncoder.encode(listOf(AbiString(s))).toHex()
        // offset 32, length 40, then 64 bytes of payload (40 + 24 zeros)
        val payload = "61".repeat(40) + "00".repeat(24)
        val expected =
            "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000028" +
                payload
        assertEquals(expected, encoded)
    }

    @Test
    fun `mixed static and dynamic args use correct offsets`() {
        // (uint256, string, uint256) — string starts at offset 96 (3 head slots),
        // length+data follow.
        val encoded = AbiEncoder.encode(
            listOf(
                AbiUint(BigInteger.valueOf(42)),
                AbiString("ab"),
                AbiUint(BigInteger.valueOf(7)),
            ),
        ).toHex()
        val expected =
            "000000000000000000000000000000000000000000000000000000000000002a" + // 42
                "0000000000000000000000000000000000000000000000000000000000000060" + // offset 96
                "0000000000000000000000000000000000000000000000000000000000000007" + // 7
                "0000000000000000000000000000000000000000000000000000000000000002" + // length 2
                "6162000000000000000000000000000000000000000000000000000000000000" // "ab" + padding
        assertEquals(expected, encoded)
    }

    @Test
    fun `empty string still occupies one length word`() {
        val encoded = AbiEncoder.encode(listOf(AbiString(""))).toHex()
        assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000000",
            encoded,
        )
    }

    @Test
    fun `invalid address length is rejected`() {
        assertFailsWith<IllegalArgumentException> { AbiAddress("0xabcd") }
    }

    @Test
    fun `bytes32 wrong size is rejected`() {
        assertFailsWith<IllegalArgumentException> { AbiBytes32(ByteArray(31)) }
        assertFailsWith<IllegalArgumentException> { AbiBytes32(ByteArray(33)) }
    }

    @Test
    fun `bytes32String rejects oversized inputs`() {
        assertFailsWith<IllegalArgumentException> { AbiEncoder.bytes32String("a".repeat(33)) }
    }

    @Test
    fun `negative uint is rejected`() {
        assertFailsWith<IllegalArgumentException> { AbiUint(BigInteger.valueOf(-1)) }
    }

    @Test
    fun `uint8 out of range is rejected`() {
        assertFailsWith<IllegalArgumentException> { AbiUint8(256) }
        assertFailsWith<IllegalArgumentException> { AbiUint8(-1) }
    }

    @Test
    fun `placeOrder argument shape produces valid calldata with right total length`() {
        // Sanity-check encoder doesn't drop bytes for the real shape we care about.
        // Layout: 10 head slots (320) + dynamic tails for 3 strings + 1 bytes32 head + …
        val args = listOf<AbiArg>(
            AbiString("a".repeat(128)),
            AbiUint(BigInteger.valueOf(5_000_000)),
            AbiAddress("0x000000000000000000000000000000000000dEaD"),
            AbiUint8(2),
            AbiString(""),
            AbiString(""),
            AbiEncoder.bytes32String("INR"),
            AbiUint(BigInteger.ZERO),
            AbiUint(BigInteger.ONE),
            AbiUint(BigInteger.ZERO),
        )
        val calldata = AbiEncoder.encodeFunctionCall(
            "placeOrder(string,uint256,address,uint8,string,string,bytes32,uint256,uint256,uint256)",
            args,
        )
        // 4 selector + (10 * 32) head + (32+128) string1 + (32) string2 + (32) string3
        val expectedLen = 4 + (10 * 32) + (32 + 128) + 32 + 32
        assertEquals(expectedLen, calldata.size, "calldata size mismatch")
        assertTrue(calldata.copyOfRange(0, 4).contentEquals("1dc46885".hexToBytes()))
    }
}

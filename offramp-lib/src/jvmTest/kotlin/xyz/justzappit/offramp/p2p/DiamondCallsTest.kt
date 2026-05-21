package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.FunctionSelector
import xyz.justzappit.evm.types.Address
import xyz.justzappit.evm.util.toHex
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiamondCallsTest {
    @Test
    fun `placeOrder calldata starts with the canonical selector and has the right length`() {
        val args = PlaceOrderArgs(
            relayPubKeyEthCrypto = "00".repeat(64),
            usdcAmount = Usdc6.ofMicros(5_000_000),
            recipientAddress = Address.parse("0x000000000000000000000000000000000000dEaD"),
            orderType = OrderType.PAY,
            currency = CurrencyCode.Inr,
            circleId = BigInteger.ONE,
        )
        val data = DiamondCalls.placeOrderCalldata(args)

        val expectedSelector = FunctionSelector.compute(
            "placeOrder(string,uint256,address,uint8,string,string,bytes32,uint256,uint256,uint256)",
        )
        assertTrue(data.copyOfRange(0, 4).contentEquals(expectedSelector))

        // 4 selector + 10 head slots + 3 dynamic string tails (each: 32 length word + padded data).
        // pubkey is 128 hex chars → 128 string bytes → 32 + 128 = 160
        // _userUpi empty string → 32
        // _userPubKey empty string → 32
        assertEquals(4 + (10 * 32) + 160 + 32 + 32, data.size)
    }

    @Test
    fun `PAY order routes the relay pubkey into _pubKey and leaves _userPubKey empty`() {
        val args = PlaceOrderArgs(
            relayPubKeyEthCrypto = "ab".repeat(64),
            usdcAmount = Usdc6.ofMicros(BigInteger.TEN),
            recipientAddress = Address.parse("0x000000000000000000000000000000000000dEaD"),
            orderType = OrderType.PAY,
            currency = CurrencyCode.Inr,
            circleId = BigInteger.ONE,
        )
        val hex = DiamondCalls.placeOrderCalldata(args).toHex()
        // The relay pubkey hex chars "ab"*64 get ASCII-encoded into the string field.
        // Each 'a' = 0x61, 'b' = 0x62, so the tail contains "6162" repeated 64 times.
        assertTrue(hex.contains("6162".repeat(64)), "expected ascii-hex of pubkey in tail")
    }

    @Test
    fun `BUY order routes the relay pubkey into _userPubKey and leaves _pubKey empty`() {
        val args = PlaceOrderArgs(
            relayPubKeyEthCrypto = "cd".repeat(64),
            usdcAmount = Usdc6.ofMicros(BigInteger.TEN),
            recipientAddress = Address.parse("0x000000000000000000000000000000000000dEaD"),
            orderType = OrderType.BUY,
            currency = CurrencyCode.Inr,
            circleId = BigInteger.ONE,
        )
        val hex = DiamondCalls.placeOrderCalldata(args).toHex()
        assertTrue(hex.contains("6364".repeat(64)), "expected ascii-hex of pubkey in tail")
    }

    @Test
    fun `setSellOrderUpi calldata starts with the canonical selector`() {
        val data = DiamondCalls.setSellOrderUpiCalldata(
            orderId = BigInteger.valueOf(42),
            encryptedUpiHex = "a".repeat(170),
        )
        val expectedSelector = FunctionSelector.compute("setSellOrderUpi(uint256,string,uint256)")
        assertTrue(data.copyOfRange(0, 4).contentEquals(expectedSelector))
        // 4 selector + 3 head slots + (32 + padded(170)) = 4 + 96 + 32 + 192 = 324
        val paddedDataLen = if (170 % 32 == 0) 170 else 170 + 32 - (170 % 32)
        assertEquals(4 + (3 * 32) + 32 + paddedDataLen, data.size)
    }

    @Test
    fun `getOrdersById calldata is selector plus one uint256`() {
        val data = DiamondCalls.getOrdersByIdCalldata(BigInteger.valueOf(42))
        assertEquals(4 + 32, data.size)
        // last byte of the uint256 should be 0x2a = 42
        assertEquals(0x2a.toByte(), data.last())
    }

    @Test
    fun `getAssignableMerchantsFromCircle calldata is selector plus 8 static words`() {
        val data = DiamondCalls.getAssignableMerchantsFromCircleCalldata(
            circleId = BigInteger.ONE,
            assignUpTo = BigInteger.valueOf(3),
            currency = CurrencyCode.Inr,
            user = Address.parse("0x000000000000000000000000000000000000bEEf"),
            usdtAmount = Usdc6.ofMicros(5_000_000),
            fiatAmount = Usdc6.ofMicros(418_000_000),
            orderType = OrderType.PAY,
        )
        assertEquals(4 + (8 * 32), data.size)
    }
}

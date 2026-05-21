package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.util.hexToBytes
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PriceConfigDecoderTest {
    @Test
    fun `decodes the live Sepolia getPriceConfig(INR) response`() {
        // Captured 2026-05-21 from eth_call to 0xce868398...532aE2 on sepolia.base.org.
        // Confirms the wire format: 4 packed uint256s, no leading offset.
        val raw = (
            "00000000000000000000000000000000000000000000000000000000056c8cc0" +
                "00000000000000000000000000000000000000000000000000000000054e0840" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000016e360"
        ).hexToBytes()

        val cfg = PriceConfigDecoder.decode(raw)
        assertEquals(Usdc6.ofMicros(91_000_000L), cfg.buyPrice)
        assertEquals(Usdc6.ofMicros(89_000_000L), cfg.sellPrice)
        assertEquals(Usdc6.ZERO, cfg.buyPriceOffset)
        assertEquals(Usdc6.ofMicros(1_500_000L), cfg.baseSpread)
    }

    @Test
    fun `sellPriceAsRate scales by 6 decimals`() {
        val cfg = PriceConfig(
            buyPrice = Usdc6.ZERO,
            sellPrice = Usdc6.ofMicros(89_178_176L),
            buyPriceOffset = Usdc6.ZERO,
            baseSpread = Usdc6.ZERO,
        )
        // compareTo because BigDecimal.equals is scale-sensitive (89.178176 != 89.17817600).
        assertEquals(0, BigDecimal("89.178176").compareTo(cfg.sellPriceAsRate()))
    }

    @Test
    fun `fiatForUsdc multiplies by the sell rate`() {
        val cfg = PriceConfig(
            buyPrice = Usdc6.ZERO,
            sellPrice = Usdc6.ofMicros(89_178_176L),
            buyPriceOffset = Usdc6.ZERO,
            baseSpread = Usdc6.ZERO,
        )
        // 5 × 89.178176 = 445.89088 → 445.89 (HALF_UP at 2dp).
        assertEquals(BigDecimal("445.89"), cfg.fiatForUsdc(BigDecimal("5")))
    }

    @Test
    fun `usdcForFiat divides by the sell rate`() {
        val cfg = PriceConfig(
            buyPrice = Usdc6.ZERO,
            sellPrice = Usdc6.ofMicros(89_178_176L),
            buyPriceOffset = Usdc6.ZERO,
            baseSpread = Usdc6.ZERO,
        )
        // 100 / 89.178176 ≈ 1.12135057… → 1.1214 (HALF_UP at 4dp).
        assertEquals(BigDecimal("1.1214"), cfg.usdcForFiat(BigDecimal("100")))
    }

    @Test
    fun `decode rejects truncated input`() {
        assertFailsWith<IllegalArgumentException> {
            PriceConfigDecoder.decode(ByteArray(127))
        }
    }
}

package xyz.justzappit.offramp.p2p

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * All four fields are typed [Usdc6] because the diamond contract returns them as 6-decimal
 * micros — even the fiat-side `sellPrice` is expressed in the same 6-decimal unit (e.g.
 * `sellPrice = 89_178_176` ⇒ 1 USDC ≈ 89.178176 fiat). Wrapping in [Usdc6] prevents accidentally
 * treating `89_178_176` as a fiat or USDC whole-token quantity at any callsite.
 */
data class PriceConfig(
    val buyPrice: Usdc6,
    val sellPrice: Usdc6,
    val buyPriceOffset: Usdc6,
    val baseSpread: Usdc6,
) {
    fun sellPriceAsRate(): BigDecimal = sellPrice.whole.stripTrailingZeros()

    fun fiatForUsdc(usdcAmount: BigDecimal, fiatScale: Int = DEFAULT_FIAT_DISPLAY_SCALE): BigDecimal =
        usdcAmount.multiply(sellPriceAsRate()).setScale(fiatScale, RoundingMode.HALF_UP)

    fun usdcForFiat(fiatAmount: BigDecimal, usdcScale: Int = DEFAULT_USDC_DISPLAY_SCALE): BigDecimal =
        fiatAmount.divide(sellPriceAsRate(), usdcScale, RoundingMode.HALF_UP)

    companion object {
        private const val DEFAULT_FIAT_DISPLAY_SCALE = 2
        private const val DEFAULT_USDC_DISPLAY_SCALE = 4
    }
}

object PriceConfigDecoder {
    // Decodes the return value of getPriceConfig(bytes32) — four packed uint256 words.
    fun decode(returnData: ByteArray): PriceConfig {
        require(returnData.size >= MIN_RETURN_BYTES) {
            "PriceConfig return data too short: ${returnData.size} bytes (need $MIN_RETURN_BYTES)"
        }
        return PriceConfig(
            buyPrice = Usdc6(word(returnData, 0)),
            sellPrice = Usdc6(word(returnData, 1)),
            buyPriceOffset = Usdc6(word(returnData, 2)),
            baseSpread = Usdc6(word(returnData, 3)),
        )
    }

    private fun word(buf: ByteArray, index: Int): BigInteger {
        val start = index * WORD
        return BigInteger(1, buf.copyOfRange(start, start + WORD))
    }

    private const val WORD = 32
    private const val FOUR_WORDS = 4
    private const val MIN_RETURN_BYTES = WORD * FOUR_WORDS
}

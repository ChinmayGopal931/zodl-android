package xyz.justzappit.offramp.p2p

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

// All values are in 6-decimal USDC units (sellPrice = 89_178_176 ⇒ 1 USDC ≈ 89.178176 fiat).
data class PriceConfig(
    val buyPrice: BigInteger,
    val sellPrice: BigInteger,
    val buyPriceOffset: BigInteger,
    val baseSpread: BigInteger,
) {
    fun sellPriceAsRate(): BigDecimal =
        BigDecimal(sellPrice).movePointLeft(SIX_DECIMALS).stripTrailingZeros()

    fun fiatForUsdc(usdcAmount: BigDecimal, fiatScale: Int = DEFAULT_FIAT_DISPLAY_SCALE): BigDecimal =
        usdcAmount.multiply(sellPriceAsRate()).setScale(fiatScale, RoundingMode.HALF_UP)

    fun usdcForFiat(fiatAmount: BigDecimal, usdcScale: Int = DEFAULT_USDC_DISPLAY_SCALE): BigDecimal =
        fiatAmount.divide(sellPriceAsRate(), usdcScale, RoundingMode.HALF_UP)

    companion object {
        private const val SIX_DECIMALS = 6
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
            buyPrice = word(returnData, 0),
            sellPrice = word(returnData, 1),
            buyPriceOffset = word(returnData, 2),
            baseSpread = word(returnData, 3),
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

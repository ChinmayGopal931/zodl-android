package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.abi.AbiDecoder
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * `sellPrice` is expressed in the same 6-decimal unit as USDC by the diamond contract (e.g.
 * `sellPrice = 89_178_176` ⇒ 1 USDC ≈ 89.178176 fiat). Wrapping in [Usdc6] prevents accidentally
 * treating the value as a fiat or USDC whole-token quantity at any callsite.
 */
data class PriceConfig(
    val sellPrice: Usdc6,
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
    /** Decodes `getPriceConfig(bytes32)` — four packed uint256 words; only `sellPrice` is surfaced. */
    fun decode(returnData: ByteArray): PriceConfig {
        val d = AbiDecoder(returnData)
        d.requireWords(FOUR_WORDS)
        return PriceConfig(sellPrice = Usdc6(d.uint(SLOT_SELL_PRICE)))
    }

    private const val SLOT_SELL_PRICE = 1
    private const val FOUR_WORDS = 4
}

/** Calls `diamondAddress.getPriceConfig(currency)` and decodes the four-word return. */
suspend fun BaseRpcClient.getPriceConfig(diamondAddress: Address, currency: CurrencyCode): PriceConfig {
    val raw = ethCall(to = diamondAddress, data = DiamondCalls.getPriceConfigCalldata(currency))
    return PriceConfigDecoder.decode(raw)
}

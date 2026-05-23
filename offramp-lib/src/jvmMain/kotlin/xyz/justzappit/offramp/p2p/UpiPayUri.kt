package xyz.justzappit.offramp.p2p

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Builds the `upi://pay?…` URI the merchant decrypts and runs through `parseQR` to settle an
 * accepted PAY order. Sending a bare VPA + `updatedAmount=0` to setSellOrderUpi makes the Diamond
 * auto-cancel the order in the same tx; the merchant pool also runs the SDK's `parseUPI` on the
 * decrypted payload, so the input must parse with at least `pa` and `am` (§6 of the findings).
 *
 * Shape mirrors `@p2pdotme/sdk` v1.1.7 / `qr-parsers/parsers/inr.ts`:
 *   upi://pay?pa=<vpa>&pn=<payee-name>&am=<inr-amount>&cu=INR
 */
object UpiPayUri {
    private const val INR_DECIMAL_PLACES = 2

    fun build(vpa: String, payeeName: String? = null, inrAmount: BigDecimal, currencyCode: String = "INR"): String {
        require(vpa.isNotBlank()) { "vpa must not be blank" }
        require(inrAmount.signum() > 0) { "inrAmount must be positive" }
        val amStr = inrAmount.setScale(INR_DECIMAL_PLACES, RoundingMode.DOWN).toPlainString()
        return buildString {
            append("upi://pay?")
            append("pa=").append(percentEncode(vpa))
            if (!payeeName.isNullOrBlank()) {
                append("&pn=").append(percentEncode(payeeName))
            }
            append("&am=").append(amStr)
            append("&cu=").append(percentEncode(currencyCode))
        }
    }

    /** Mirrors `parseAmount` in the SDK: floor(fiat / sellPrice, 6) — never round up. */
    fun parsedUsdcMicros(inrAmount: BigDecimal, sellPriceInrPerUsdc: BigDecimal): Long {
        require(sellPriceInrPerUsdc.signum() > 0) { "sellPrice must be positive" }
        val usdc = inrAmount.divide(sellPriceInrPerUsdc, Usdc6.DECIMALS, RoundingMode.DOWN)
        return usdc.movePointRight(Usdc6.DECIMALS).toLong()
    }

    private fun percentEncode(s: String): String {
        val out = StringBuilder(s.length)
        for (c in s) {
            if (
                c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' ||
                c == '-' || c == '_' || c == '.' || c == '~' || c == '@'
            ) {
                out.append(c)
            } else {
                for (b in c.toString().toByteArray(Charsets.UTF_8)) {
                    out.append('%')
                    out.append(HEX[(b.toInt() shr HEX_NIBBLE) and HEX_MASK])
                    out.append(HEX[b.toInt() and HEX_MASK])
                }
            }
        }
        return out.toString()
    }

    private const val HEX_NIBBLE = 4
    private const val HEX_MASK = 0xF
    private val HEX = "0123456789ABCDEF".toCharArray()
}

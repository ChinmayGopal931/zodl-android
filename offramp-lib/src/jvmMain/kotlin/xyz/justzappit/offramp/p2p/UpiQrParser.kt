package xyz.justzappit.offramp.p2p

import java.math.BigDecimal

/**
 * Validates and parses INR/UPI payment QR codes. Two regexes intentionally diverge — they mirror
 * the split in `@p2pdotme/sdk` v1.1.7:
 *
 * - [VALIDATE_REGEX] is the **strict** form (mirrors `country/currencies/inr.ts validateUPIId`):
 *   2-256 chars local part, 2-64 alphanumeric bank handle. Use for user-typed UPI IDs entered
 *   into the form. Examples that must pass: `john@paytm`, `user.name@ybl`, `8658404239@kotak811`.
 *
 * - [PARSE_REGEX] is the **lenient** form (mirrors `qr-parsers/parsers/inr.ts UPI_ID_REGEX`):
 *   allows `.` and `-` in the bank handle. Use only for the `pa` parameter pulled out of a
 *   scanned `upi://pay?...` URI, where real-world merchant QRs occasionally include those.
 *
 * Keeping both lets the form be strict on what the user types while still accepting merchant
 * QRs the SDK would accept. Diverging from these regexes will cause some merchant payments to
 * fail on the SDK side after we've already broadcast `placeOrder`, so do NOT relax either
 * without coordinating with the p2p.me SDK.
 */
object UpiQrParser {
    private val VALIDATE_REGEX = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9]{2,64}$")
    private val PARSE_REGEX = Regex("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$")

    /**
     * Strict validation for a UPI ID typed into the form. Mirrors `validateUPIId` in
     * `@p2pdotme/sdk` v1.1.7 byte-for-byte.
     */
    fun validateUpiId(upiId: String): Boolean {
        val trimmed = upiId.trim()
        if (trimmed.isEmpty()) return false
        return VALIDATE_REGEX.matches(trimmed)
    }

    /**
     * Parses a scanned UPI QR payload. Accepts:
     * - Full URI: `upi://pay?pa=merchant@upi&pn=Name&am=250&cu=INR`
     * - Bare query string: `pa=merchant@upi&am=100`
     * - Trimmed whitespace.
     *
     * Returns a [UpiQrParseResult] sum type so callers can pattern-match on the failure mode
     * (UI surfaces a different message per case).
     */
    fun parseQr(qrData: String): UpiQrParseResult {
        val trimmed = qrData.trim()
        if (trimmed.isEmpty()) {
            return UpiQrParseResult.Failure(UpiQrError.EmptyQr)
        }

        val paramString = when {
            trimmed.startsWith(UPI_URI_PREFIX, ignoreCase = true) -> trimmed.substring(UPI_URI_PREFIX.length)
            trimmed.contains('?') -> trimmed.substringAfter('?')
            else -> trimmed
        }

        val params = parseQueryParams(paramString)
        val paymentAddress = params["pa"] ?: return UpiQrParseResult.Failure(UpiQrError.MissingPaymentAddress)

        if (!PARSE_REGEX.matches(paymentAddress)) {
            return UpiQrParseResult.Failure(UpiQrError.InvalidUpiId(paymentAddress))
        }

        val amount = params["am"]?.let { amountStr ->
            val parsed = runCatching { BigDecimal(amountStr.trim()) }.getOrNull()
            if (parsed == null || parsed.signum() <= 0) {
                return UpiQrParseResult.Failure(UpiQrError.InvalidAmount(amountStr))
            }
            parsed
        }

        return UpiQrParseResult.Success(
            ParsedUpiQr(paymentAddress = paymentAddress, fiatAmount = amount),
        )
    }

    /**
     * Hand-rolled `application/x-www-form-urlencoded` decoder so the parser stays
     * Android-runtime-free (no `java.net.URLDecoder` is fine on JVM, but we avoid `Uri.parse`
     * to keep this in offramp-lib jvmMain). Handles `+` → space and `%XX` byte escapes.
     */
    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, String>()
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val key = if (eq < 0) pair else pair.substring(0, eq)
            val value = if (eq < 0) "" else pair.substring(eq + 1)
            if (key.isEmpty()) continue
            out[urlDecode(key)] = urlDecode(value)
        }
        return out
    }

    private fun urlDecode(s: String): String {
        if (s.isEmpty()) return s
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            when (val c = s[i]) {
                '+' -> { out.append(' '); i++ }
                '%' -> if (i + 2 < s.length) {
                    val hi = Character.digit(s[i + 1], HEX_BASE)
                    val lo = Character.digit(s[i + 2], HEX_BASE)
                    if (hi >= 0 && lo >= 0) {
                        out.append(((hi shl HEX_NIBBLE) + lo).toChar())
                        i += PERCENT_ESCAPE_LEN
                    } else {
                        out.append(c); i++
                    }
                } else {
                    out.append(c); i++
                }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    private const val UPI_URI_PREFIX = "upi://pay?"
    private const val HEX_BASE = 16
    private const val HEX_NIBBLE = 4
    private const val PERCENT_ESCAPE_LEN = 3
}

data class ParsedUpiQr(
    val paymentAddress: String,
    val fiatAmount: BigDecimal?,
)

sealed class UpiQrError {
    object EmptyQr : UpiQrError()
    object MissingPaymentAddress : UpiQrError()
    data class InvalidUpiId(val raw: String) : UpiQrError()
    data class InvalidAmount(val raw: String) : UpiQrError()
}

sealed class UpiQrParseResult {
    data class Success(val parsed: ParsedUpiQr) : UpiQrParseResult()
    data class Failure(val error: UpiQrError) : UpiQrParseResult()
}

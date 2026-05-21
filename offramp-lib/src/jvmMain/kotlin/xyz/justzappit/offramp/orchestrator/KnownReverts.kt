package xyz.justzappit.offramp.orchestrator

/**
 * Translates raw 4-byte EVM error selectors and revert payloads into human-readable
 * messages. Maintained from Discord observations (see `docs/integrations/UPI_Offramp.md` §2)
 * and from the p2p.me ABI errors.
 */
object KnownReverts {

    /**
     * Selector → user-facing reason mapping. Selectors are the first 4 bytes of
     * keccak256(canonicalErrorSignature) — same shape as function selectors.
     */
    private val SELECTOR_LOOKUP: Map<String, String> = mapOf(
        "0x91da284f" to
            "Insufficient reputation points. BUY orders (and possibly PAY orders for fresh " +
            "addresses) require an RP grant from the p2p.me team on Sepolia, or social/KYC " +
            "verification on mainnet.",
        "0x5d04ff4c" to
            "No merchant has fiat liquidity for this order in the selected circle. Try a " +
            "smaller amount, wait for merchants to fund, or retry to pick a different circle.",
        "0x08c379a0" to
            // Standard Solidity Error(string) — actual reason follows as ABI-encoded string.
            "Contract revert with a string reason (see decodedReason for the message).",
    )

    /** Extracts the first `0x........` 8-hex selector found in [rawError], or null. */
    fun extractSelector(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null
        val match = SELECTOR_PATTERN.find(rawError) ?: return null
        return match.value.lowercase()
    }

    /** Looks up the human-readable reason for a known selector. */
    fun explain(selector: String?): String? = selector?.lowercase()?.let { SELECTOR_LOOKUP[it] }

    /**
     * Best-effort decode of an `Error(string)` revert (selector 0x08c379a0) payload to
     * its embedded message. Returns null if the layout doesn't match.
     */
    fun decodeErrorString(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null
        val payload = ERROR_STRING_PATTERN.find(rawError)?.value ?: return null
        val hex = payload.removePrefix("0x08c379a0").take(MAX_ERROR_PAYLOAD_HEX)
        // Skip head (offset=0x20) + length, then read length-bytes of UTF-8.
        if (hex.length < HEAD_HEX_LEN) return null
        val lengthHex = hex.substring(OFFSET_HEX_LEN, HEAD_HEX_LEN)
        val length = runCatching { lengthHex.toInt(HEX_BASE) }.getOrNull() ?: return null
        if (length <= 0 || length > MAX_STRING_BYTES) return null
        val dataStart = HEAD_HEX_LEN
        val dataEnd = dataStart + (length * 2)
        if (hex.length < dataEnd) return null
        val data = hex.substring(dataStart, dataEnd)
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            bytes[i] = (data.substring(i * 2, i * 2 + 2).toInt(HEX_BASE) and 0xff).toByte()
        }
        return runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
    }

    private val SELECTOR_PATTERN = Regex("0x[0-9a-fA-F]{8}(?![0-9a-fA-F])")
    private val ERROR_STRING_PATTERN = Regex("0x08c379a0[0-9a-fA-F]{128,}")
    private const val HEX_BASE = 16
    private const val OFFSET_HEX_LEN = 64
    private const val HEAD_HEX_LEN = 128
    private const val MAX_STRING_BYTES = 1024
    private const val MAX_ERROR_PAYLOAD_HEX = 8 + (HEAD_HEX_LEN + MAX_STRING_BYTES * 2)
}

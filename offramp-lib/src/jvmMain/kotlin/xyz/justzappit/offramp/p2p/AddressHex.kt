package xyz.justzappit.offramp.p2p

/**
 * Normalises a hex-encoded address (from any source — subgraph `Bytes`, on-chain ABI slot)
 * into a canonical lowercase `0x` + 40-hex form. Returns `null` if the input represents
 * the zero address (any length of all-zero hex), is blank, or fails to parse.
 *
 * The subgraph's `Bytes` scalar omits leading zeros, so `0x00000000` (4 hex chars) and
 * `0x0000000000000000000000000000000000000000` are both the zero address and both map
 * to `null` here.
 */
internal fun parseNullableAddress(hex: String?): String? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.removePrefix("0x").lowercase()
    if (cleaned.isEmpty()) return null
    if (cleaned.all { it == '0' }) return null
    if (!cleaned.all { it.isAsciiHexDigit() }) return null
    if (cleaned.length > ADDRESS_HEX_LEN) return null
    return "0x" + cleaned.padStart(ADDRESS_HEX_LEN, '0')
}

internal fun parseEpochSecondsOrNull(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val parsed = value.toLongOrNull() ?: return null
    return if (parsed == 0L) null else parsed
}

private fun Char.isAsciiHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private const val ADDRESS_HEX_LEN = 40

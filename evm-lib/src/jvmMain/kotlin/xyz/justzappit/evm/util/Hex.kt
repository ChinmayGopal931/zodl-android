package xyz.justzappit.evm.util

import java.math.BigInteger

/**
 * Parses a hex string (with or without `0x` prefix) as an unsigned [BigInteger]. Empty or `"0x"`
 * → [BigInteger.ZERO]. Used everywhere RPC responses return hex-encoded numeric fields
 * (eth_chainId, eth_gasPrice, block.baseFeePerGas, UserOp gas limits, …).
 */
fun hexToBigInteger(hex: String): BigInteger {
    val s = hex.removePrefix("0x")
    return if (s.isEmpty()) BigInteger.ZERO else BigInteger(s, 16)
}

// Error messages must not echo the input: callers pass private-key hex (Ecies.decryptWithPrivateKey,
// the offramp relay identity), and a malformed persisted key would otherwise land in logged exceptions.
fun String.hexToBytes(): ByteArray {
    val raw = if (startsWith("0x") || startsWith("0X")) substring(2) else this
    require(raw.length % 2 == 0) { "hex input must have even length, got ${raw.length}" }
    val out = ByteArray(raw.length / 2)
    var i = 0
    while (i < raw.length) {
        val hi = Character.digit(raw[i], 16)
        val lo = Character.digit(raw[i + 1], 16)
        require(hi >= 0 && lo >= 0) { "hex input contains non-hex character at index $i" }
        out[i / 2] = ((hi shl 4) + lo).toByte()
        i += 2
    }
    return out
}

fun ByteArray.toHex(): String =
    buildString(size * 2) {
        for (b in this@toHex) {
            val v = b.toInt() and 0xff
            append(Character.forDigit(v ushr 4, 16))
            append(Character.forDigit(v and 0x0f, 16))
        }
    }

package xyz.justzappit.evm.util

fun String.hexToBytes(): ByteArray {
    val raw = if (startsWith("0x") || startsWith("0X")) substring(2) else this
    require(raw.length % 2 == 0) { "hex input must have even length, got ${raw.length}: '$this'" }
    val out = ByteArray(raw.length / 2)
    var i = 0
    while (i < raw.length) {
        val hi = Character.digit(raw[i], 16)
        val lo = Character.digit(raw[i + 1], 16)
        require(hi >= 0 && lo >= 0) { "hex input contains non-hex character at index $i: '$this'" }
        out[i / 2] = ((hi shl 4) + lo).toByte()
        i += 2
    }
    return out
}

fun ByteArray.toHex(): String = buildString(size * 2) {
    for (b in this@toHex) {
        val v = b.toInt() and 0xff
        append(Character.forDigit(v ushr 4, 16))
        append(Character.forDigit(v and 0x0f, 16))
    }
}

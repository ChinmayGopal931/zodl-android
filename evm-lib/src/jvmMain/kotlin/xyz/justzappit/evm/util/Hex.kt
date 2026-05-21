package xyz.justzappit.evm.util

fun String.hexToBytes(): ByteArray {
    val s = if (length % 2 == 1) "0$this" else this
    val out = ByteArray(s.length / 2)
    var i = 0
    while (i < s.length) {
        out[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
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

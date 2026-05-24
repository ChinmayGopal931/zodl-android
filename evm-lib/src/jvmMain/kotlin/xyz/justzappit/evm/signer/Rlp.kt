package xyz.justzappit.evm.signer

import java.io.ByteArrayOutputStream
import java.math.BigInteger

sealed interface RlpItem {
    @JvmInline
    value class Bytes(
        val value: ByteArray
    ) : RlpItem

    @JvmInline
    value class L(
        val items: List<RlpItem>
    ) : RlpItem
}

fun rlpBytes(b: ByteArray): RlpItem = RlpItem.Bytes(b)

fun rlpEmpty(): RlpItem = RlpItem.Bytes(ByteArray(0))

fun rlpInt(v: BigInteger): RlpItem {
    if (v.signum() == 0) return rlpEmpty()
    require(v.signum() > 0) { "RLP integers must be non-negative" }
    return RlpItem.Bytes(stripLeadingZeros(v.toByteArray()))
}

fun rlpInt(v: Long): RlpItem = rlpInt(BigInteger.valueOf(v))

fun rlpList(vararg items: RlpItem): RlpItem = RlpItem.L(items.toList())

fun rlpList(items: List<RlpItem>): RlpItem = RlpItem.L(items)

object Rlp {
    fun encode(item: RlpItem): ByteArray {
        val out = ByteArrayOutputStream()
        writeItem(out, item)
        return out.toByteArray()
    }

    private fun writeItem(out: ByteArrayOutputStream, item: RlpItem) {
        when (item) {
            is RlpItem.Bytes -> writeBytes(out, item.value)
            is RlpItem.L -> writeList(out, item.items)
        }
    }

    private fun writeBytes(out: ByteArrayOutputStream, bytes: ByteArray) {
        if (bytes.size == 1 && (bytes[0].toInt() and 0xff) < 0x80) {
            out.write(bytes[0].toInt())
            return
        }
        writeLength(out, bytes.size, 0x80)
        out.write(bytes)
    }

    private fun writeList(out: ByteArrayOutputStream, items: List<RlpItem>) {
        val inner =
            ByteArrayOutputStream()
                .apply {
                    items.forEach { writeItem(this, it) }
                }.toByteArray()
        writeLength(out, inner.size, 0xc0)
        out.write(inner)
    }

    private fun writeLength(out: ByteArrayOutputStream, length: Int, offset: Int) {
        if (length < SHORT_LENGTH_THRESHOLD) {
            out.write(offset + length)
        } else {
            val lenBytes = stripLeadingZeros(BigInteger.valueOf(length.toLong()).toByteArray())
            out.write(offset + SHORT_LENGTH_THRESHOLD + lenBytes.size - 1)
            out.write(lenBytes)
        }
    }

    private const val SHORT_LENGTH_THRESHOLD = 56
}

private fun stripLeadingZeros(b: ByteArray): ByteArray {
    var i = 0
    while (i < b.size - 1 && b[i] == 0.toByte()) i++
    return if (i == 0) b else b.copyOfRange(i, b.size)
}

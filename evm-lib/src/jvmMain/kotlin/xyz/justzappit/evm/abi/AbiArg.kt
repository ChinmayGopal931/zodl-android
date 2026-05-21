package xyz.justzappit.evm.abi

import xyz.justzappit.evm.types.Address
import java.math.BigInteger

sealed interface AbiArg {
    val isDynamic: Boolean
    fun head(): ByteArray
    fun tail(): ByteArray
}

data class AbiUint(val value: BigInteger) : AbiArg {
    init {
        require(value.signum() >= 0) { "uint must be non-negative, got $value" }
        require(value.bitLength() <= MAX_UINT_BITS) { "uint exceeds 256 bits" }
    }
    override val isDynamic = false
    override fun head(): ByteArray = padTo32Left(value.toByteArray())
    override fun tail(): ByteArray = EMPTY
}

data class AbiInt(val value: BigInteger) : AbiArg {
    init {
        // bitLength() excludes the sign bit, so a signed int256 in [-2^255, 2^255-1] has
        // bitLength <= 255; anything wider would silently wrap under two's complement below.
        require(value.bitLength() <= MAX_INT_SIGNED_BITS) { "int256 out of range, got $value" }
    }
    override val isDynamic = false
    override fun head(): ByteArray {
        if (value.signum() >= 0) return padTo32Left(value.toByteArray())
        // Two's complement: 2^256 + value
        val twos = BigInteger.ONE.shiftLeft(MAX_UINT_BITS).add(value)
        return padTo32Left(twos.toByteArray())
    }
    override fun tail(): ByteArray = EMPTY
}

data class AbiAddress(val address: Address) : AbiArg {
    override val isDynamic = false
    override fun head(): ByteArray = ByteArray(WORD - ADDRESS_BYTES) + address.bytes
    override fun tail(): ByteArray = EMPTY
}

data class AbiBytes32(val value: ByteArray) : AbiArg {
    init {
        require(value.size == WORD) { "bytes32 must be 32 bytes, got ${value.size}" }
    }
    override val isDynamic = false
    override fun head(): ByteArray = value.copyOf()
    override fun tail(): ByteArray = EMPTY
    override fun equals(other: Any?): Boolean = other is AbiBytes32 && value.contentEquals(other.value)
    override fun hashCode(): Int = value.contentHashCode()
}

data class AbiUint8(val value: Int) : AbiArg {
    init {
        require(value in 0..UINT8_MAX) { "uint8 out of range: $value" }
    }
    override val isDynamic = false
    override fun head(): ByteArray = padTo32Left(byteArrayOf(value.toByte()))
    override fun tail(): ByteArray = EMPTY
}

data class AbiBool(val value: Boolean) : AbiArg {
    override val isDynamic = false
    override fun head(): ByteArray {
        val out = ByteArray(WORD)
        if (value) out[out.size - 1] = 1
        return out
    }
    override fun tail(): ByteArray = EMPTY
}

data class AbiString(val value: String) : AbiArg {
    val bytes: ByteArray = value.toByteArray(Charsets.UTF_8)
    override val isDynamic = true
    override fun head(): ByteArray = ByteArray(WORD)
    override fun tail(): ByteArray {
        val out = ByteArray(WORD + padded(bytes.size))
        // Length prefix
        val lenBytes = BigInteger.valueOf(bytes.size.toLong()).toByteArray()
        System.arraycopy(lenBytes, 0, out, WORD - lenBytes.size, lenBytes.size)
        // Data, right-padded with zeros
        System.arraycopy(bytes, 0, out, WORD, bytes.size)
        return out
    }
    override fun equals(other: Any?): Boolean = other is AbiString && value == other.value
    override fun hashCode(): Int = value.hashCode()
}

data class AbiBytes(val value: ByteArray) : AbiArg {
    override val isDynamic = true
    override fun head(): ByteArray = ByteArray(WORD)
    override fun tail(): ByteArray {
        val out = ByteArray(WORD + padded(value.size))
        val lenBytes = BigInteger.valueOf(value.size.toLong()).toByteArray()
        System.arraycopy(lenBytes, 0, out, WORD - lenBytes.size, lenBytes.size)
        System.arraycopy(value, 0, out, WORD, value.size)
        return out
    }
    override fun equals(other: Any?): Boolean = other is AbiBytes && value.contentEquals(other.value)
    override fun hashCode(): Int = value.contentHashCode()
}

internal fun padTo32Left(b: ByteArray): ByteArray = when {
    b.size == WORD -> b
    b.size > WORD -> b.copyOfRange(b.size - WORD, b.size)
    else -> ByteArray(WORD).also { System.arraycopy(b, 0, it, WORD - b.size, b.size) }
}

internal fun padded(size: Int): Int = if (size % WORD == 0) size else size + WORD - (size % WORD)

internal const val WORD = 32
private const val MAX_UINT_BITS = 256
private const val MAX_INT_SIGNED_BITS = 255
private const val ADDRESS_BYTES = 20
private const val UINT8_MAX = 255
private val EMPTY = ByteArray(0)

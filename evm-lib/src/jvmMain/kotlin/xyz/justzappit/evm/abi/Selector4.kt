package xyz.justzappit.evm.abi

import xyz.justzappit.evm.util.hexToBytes
import xyz.justzappit.evm.util.toHex

class Selector4(
    bytes: ByteArray
) {
    val bytes: ByteArray = bytes.copyOf()

    init {
        require(this.bytes.size == LEN) {
            "Selector4 must be 4 bytes, got ${this.bytes.size}"
        }
    }

    val hex: String get() = "0x" + bytes.toHex()

    override fun toString(): String = hex

    override fun equals(other: Any?): Boolean =
        this === other || (other is Selector4 && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        const val LEN = 4

        fun fromHex(hex: String): Selector4 = Selector4(hex.hexToBytes())

        fun fromCanonicalSignature(signature: String): Selector4 =
            Selector4(
                keccak256(signature.toByteArray(Charsets.US_ASCII)).copyOf(LEN),
            )

        fun fromBytesPrefix(bytes: ByteArray): Selector4? =
            if (bytes.size >= LEN) Selector4(bytes.copyOfRange(0, LEN)) else null
    }
}

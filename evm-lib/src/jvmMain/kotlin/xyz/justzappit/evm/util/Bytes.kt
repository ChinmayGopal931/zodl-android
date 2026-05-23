package xyz.justzappit.evm.util

private const val WORD = 32

/**
 * Left-pads (or left-truncates) [this] to a 32-byte EVM word. Used by ABI encoding, ECDSA
 * scalar/coordinate framing, and HD-key derivation — anywhere a `BigInteger.toByteArray()` result
 * needs to land in the canonical 32-byte slot. Truncation removes the high bytes (matching the
 * `unchecked uintN(value)` semantics of solc for in-range values).
 */
fun ByteArray.padLeftToWord(): ByteArray = when {
    size == WORD -> this
    size > WORD -> copyOfRange(size - WORD, size)
    else -> ByteArray(WORD).also { System.arraycopy(this, 0, it, WORD - size, size) }
}

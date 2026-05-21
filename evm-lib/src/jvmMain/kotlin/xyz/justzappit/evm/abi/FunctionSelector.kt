package xyz.justzappit.evm.abi

import xyz.justzappit.evm.util.toHex

object FunctionSelector {
    fun compute(canonicalSignature: String): ByteArray =
        keccak256(canonicalSignature.toByteArray(Charsets.US_ASCII)).copyOfRange(0, SELECTOR_BYTES)

    fun computeHex(canonicalSignature: String): String = compute(canonicalSignature).toHex()

    private const val SELECTOR_BYTES = 4
}

package xyz.justzappit.evm.types

import kotlinx.serialization.Serializable

/**
 * EVM chain id. Typed to prevent mixing it up with nonces, block numbers, or other Long-shaped
 * values in transaction construction.
 */
@JvmInline
@Serializable
value class ChainId(
    val value: Long
) {
    init {
        require(value > 0) { "ChainId must be positive, got $value" }
    }

    val hex: String get() = "0x" + value.toString(HEX_BASE)

    override fun toString(): String = value.toString()

    companion object {
        private const val HEX_BASE = 16

        val BASE_SEPOLIA: ChainId = ChainId(84_532L)
        val BASE_MAINNET: ChainId = ChainId(8_453L)
    }
}

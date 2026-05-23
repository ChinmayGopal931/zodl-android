package xyz.justzappit.evm.types

import java.math.BigInteger

/**
 * A gas-unit count (a gas limit or estimate). Typed apart from [Wei] (price-per-gas / value) and
 * [Nonce] so fee math can't accidentally treat a gas count as a wei amount.
 */
@JvmInline
value class Gas(val value: BigInteger) {
    init {
        require(value.signum() >= 0) { "Gas must be non-negative, got $value" }
    }

    operator fun times(scalar: BigInteger): Gas = Gas(value * scalar)
    operator fun div(scalar: BigInteger): Gas = Gas(value / scalar)
}

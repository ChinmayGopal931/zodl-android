package xyz.justzappit.evm.types

import java.math.BigInteger

/**
 * An account transaction nonce — a monotonic per-account counter. Typed apart from [Wei] amounts
 * and [Gas] unit-counts so the three can't be transposed in transaction assembly.
 */
@JvmInline
value class Nonce(val value: BigInteger) {
    init {
        require(value.signum() >= 0) { "Nonce must be non-negative, got $value" }
    }
}

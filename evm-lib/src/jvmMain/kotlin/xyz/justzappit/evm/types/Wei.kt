package xyz.justzappit.evm.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

/**
 * Atomic wei amount — i.e. ETH * 10^18 on Ethereum / Base / any EVM chain. Distinguished from
 * gas-unit counts (`gasLimit`) and nonces by type so that fee math at the signer layer can't
 * accidentally mix the three.
 */
@Serializable(with = Wei.WeiSerializer::class)
@JvmInline
value class Wei(
    val value: BigInteger
) {
    init {
        require(value.signum() >= 0) { "Wei must be non-negative, got $value" }
    }

    operator fun plus(other: Wei): Wei = Wei(value + other.value)

    operator fun times(scalar: Int): Wei = Wei(value * BigInteger.valueOf(scalar.toLong()))

    override fun toString(): String = "${value}wei"

    companion object {
        val ZERO: Wei = Wei(BigInteger.ZERO)

        fun ofLong(value: Long): Wei = Wei(BigInteger.valueOf(value))
    }

    object WeiSerializer : KSerializer<Wei> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("xyz.justzappit.evm.types.Wei", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Wei = Wei(BigInteger(decoder.decodeString()))

        override fun serialize(encoder: Encoder, value: Wei) = encoder.encodeString(value.value.toString())
    }
}

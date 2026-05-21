package xyz.justzappit.offramp.p2p

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.BigInteger

/**
 * A 6-decimal token amount, stored in micro-units (1 USDC = 1_000_000 micro-USDC). Used for both
 * USDC and any fiat amount the p2p.me diamond expresses in the same 6-decimal currency unit (see
 * [PriceConfig]).
 *
 * The point of this wrapper is to make "is this `5_000_000` micros or `5` USDC?" un-confusable at
 * every callsite. Construct via [Usdc6.ofMicros] (raw on-chain integer) or [Usdc6.ofWhole]
 * (decimal). Mixing the two without an explicit conversion is a compile error.
 */
@Serializable(with = Usdc6.Usdc6Serializer::class)
@JvmInline
value class Usdc6(val micros: BigInteger) : Comparable<Usdc6> {

    val whole: BigDecimal get() = BigDecimal(micros).movePointLeft(DECIMALS)

    operator fun plus(other: Usdc6): Usdc6 = Usdc6(micros + other.micros)
    operator fun minus(other: Usdc6): Usdc6 = Usdc6(micros - other.micros)
    override fun compareTo(other: Usdc6): Int = micros.compareTo(other.micros)

    override fun toString(): String = "$whole(=${micros}µ)"

    companion object {
        const val DECIMALS: Int = 6

        val ZERO: Usdc6 = Usdc6(BigInteger.ZERO)

        /** Construct from raw 6-decimal micros (the wire format used by the diamond contract). */
        fun ofMicros(micros: BigInteger): Usdc6 = Usdc6(micros)

        fun ofMicros(micros: Long): Usdc6 = Usdc6(BigInteger.valueOf(micros))

        /**
         * Construct from a whole-token decimal amount (i.e. 5.50 USDC). Rounds half-up at the
         * sixth decimal place; values with more than 6 decimals lose precision silently — pass an
         * already-rounded [BigDecimal] if that matters at the callsite.
         */
        fun ofWhole(whole: BigDecimal): Usdc6 =
            Usdc6(whole.movePointRight(DECIMALS).toBigInteger())
    }

    object Usdc6Serializer : KSerializer<Usdc6> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("xyz.justzappit.offramp.p2p.Usdc6", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Usdc6 = Usdc6(BigInteger(decoder.decodeString()))
        override fun serialize(encoder: Encoder, value: Usdc6) = encoder.encodeString(value.micros.toString())
    }
}

package xyz.justzappit.offramp.p2p

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Fiat currency code supported by the UPI offramp. Today only [Inr] is wired through; the type
 * exists to prevent magic-string "INR" leaking through the orchestrator + UI layers.
 */
@Serializable
enum class CurrencyCode(val code: String) {
    Inr("INR"),
    ;

    override fun toString(): String = code

    companion object {
        fun fromCode(code: String): CurrencyCode = fromCodeOrNull(code)
            ?: throw IllegalArgumentException("Unknown currency code: '$code'")

        fun fromCodeOrNull(code: String): CurrencyCode? {
            val normalised = code.uppercase(Locale.ROOT)
            return values().firstOrNull { it.code == normalised }
        }
    }
}

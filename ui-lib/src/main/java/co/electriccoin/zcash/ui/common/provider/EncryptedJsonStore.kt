package co.electriccoin.zcash.ui.common.provider

import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Shared encrypted-prefs JSON store. Both [OfframpCheckpointStorageProvider] and
 * [RelayIdentityStorageProvider] delegate to this — they keep distinct interfaces but reuse the
 * same serialize/encrypted-write/decode plumbing.
 *
 * **Absent vs corrupt — read this before touching `decode`.**
 *
 * The earlier implementation swallowed `SerializationException` and returned `null` for any
 * decode failure, conflating two very different states:
 *
 *  - The key has never been written → reasonable to return `null` and let the caller create.
 *  - The key was written but the blob is now corrupt → returning `null` causes the caller's
 *    `getOrCreate`-style path to silently overwrite the (still-encrypted) original with a fresh
 *    value. For the relay identity that means every past order's `encMerchantUpi` becomes
 *    permanently undecryptable. For the offramp checkpoint that means the UI thinks "no in-flight
 *    order" while the chain still holds an escrowed sell-order.
 *
 * `decode` now throws [StoreCorruptedException] on a present-but-undecodable value. Callers can
 * choose to log + clear + regenerate (an explicit recovery decision) or to fail closed; what they
 * cannot do is silently overwrite without realising data was lost.
 *
 * Schema migration: when we eventually need one, version the key (e.g. `_v2`) and migrate
 * explicitly, rather than relying on tolerant decoding that erases the old shape.
 */
internal class EncryptedJsonStore<T>(
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider,
    prefKey: String,
    private val serializer: KSerializer<T>,
) {
    private val key = PreferenceKey(prefKey)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    /** Returns null when the key is absent. Throws [StoreCorruptedException] when present-but-undecodable. */
    suspend fun get(): T? = encryptedPreferenceProvider().getString(key)?.let(::decode)

    suspend fun set(value: T) {
        encryptedPreferenceProvider().putString(key, json.encodeToString(serializer, value))
    }

    suspend fun clear() {
        encryptedPreferenceProvider().putString(key, null)
    }

    /**
     * Observes the stored value. Emits `null` on absent, `T` on decoded, and throws
     * [StoreCorruptedException] inside the flow when a present blob fails to decode — so a
     * `LaunchedEffect` consumer's `catch { }` operator sees corruption distinctly from absence.
     */
    fun observe(): Flow<T?> =
        flow {
            emitAll(encryptedPreferenceProvider().observe(key).map { raw -> raw?.let(::decode) })
        }

    private fun decode(raw: String): T =
        try {
            json.decodeFromString(serializer, raw)
        } catch (e: SerializationException) {
            throw StoreCorruptedException(
                "Encrypted-prefs blob for key ${key.key} present but failed to decode " +
                    "(${e.message}). Refusing to silently treat as absent — caller must decide " +
                    "between clear+regenerate and fail-closed.",
                e,
            )
        }
}

/**
 * Thrown by [EncryptedJsonStore] when the stored blob is present but cannot be decoded. Distinct
 * from a `null` return (which is the absent case). Callers that *want* a clear-and-regenerate on
 * corruption should catch this explicitly — never blanket-swallow it.
 */
class StoreCorruptedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

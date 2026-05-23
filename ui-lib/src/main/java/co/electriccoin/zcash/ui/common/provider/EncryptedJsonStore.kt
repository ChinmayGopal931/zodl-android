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
 * same serialize/encrypted-write/decode-tolerant-of-drift plumbing.
 */
internal class EncryptedJsonStore<T>(
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider,
    prefKey: String,
    private val serializer: KSerializer<T>,
) {
    private val key = PreferenceKey(prefKey)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun get(): T? = encryptedPreferenceProvider().getString(key)?.let(::decode)

    suspend fun set(value: T) {
        encryptedPreferenceProvider().putString(key, json.encodeToString(serializer, value))
    }

    suspend fun clear() {
        encryptedPreferenceProvider().putString(key, null)
    }

    fun observe(): Flow<T?> = flow {
        emitAll(encryptedPreferenceProvider().observe(key).map { raw -> raw?.let(::decode) })
    }

    // Schema drift between fork versions: drop the stale value rather than crashing. Any other
    // failure (e.g. IllegalArgumentException from invariant violations) is a real bug and must
    // surface — do NOT swallow it here.
    private fun decode(raw: String): T? = try {
        json.decodeFromString(serializer, raw)
    } catch (_: SerializationException) {
        null
    }
}

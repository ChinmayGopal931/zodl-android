package co.electriccoin.zcash.ui.common.provider

import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import xyz.justzappit.offramp.orchestrator.OfframpCheckpoint

/**
 * Persists the single in-flight UPI offramp checkpoint so a process death or config change between
 * on-chain broadcasts doesn't orphan the user's USDC. v1 holds one slot; the API shape supports
 * lifting that limit later without changing the wire format.
 */
interface OfframpCheckpointStorageProvider {
    suspend fun get(): OfframpCheckpoint?
    suspend fun store(checkpoint: OfframpCheckpoint)
    suspend fun clear()
    fun observe(): Flow<OfframpCheckpoint?>
}

internal class OfframpCheckpointStorageProviderImpl(
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider,
) : OfframpCheckpointStorageProvider {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val key = PreferenceKey(PREF_KEY)

    override suspend fun get(): OfframpCheckpoint? =
        encryptedPreferenceProvider().getString(key)?.let(::decode)

    override suspend fun store(checkpoint: OfframpCheckpoint) {
        encryptedPreferenceProvider().putString(
            key,
            json.encodeToString(OfframpCheckpoint.serializer(), checkpoint),
        )
    }

    override suspend fun clear() {
        encryptedPreferenceProvider().putString(key, null)
    }

    override fun observe(): Flow<OfframpCheckpoint?> = flow {
        emitAll(encryptedPreferenceProvider().observe(key).map { raw -> raw?.let(::decode) })
    }

    private fun decode(raw: String): OfframpCheckpoint? = try {
        json.decodeFromString(OfframpCheckpoint.serializer(), raw)
    } catch (e: SerializationException) {
        // Schema drift between fork versions: drop the stale checkpoint rather than crashing.
        // Any other failure (e.g. IllegalArgumentException from invariant violations) is a real
        // bug and must surface — do NOT swallow it here.
        null
    }

    companion object {
        private const val PREF_KEY = "upi_offramp_checkpoint_v1"
    }
}

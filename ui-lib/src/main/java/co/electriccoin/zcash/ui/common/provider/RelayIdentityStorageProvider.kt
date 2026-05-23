package co.electriccoin.zcash.ui.common.provider

import co.electriccoin.zcash.preference.EncryptedPreferenceProvider
import co.electriccoin.zcash.preference.model.entry.PreferenceKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import xyz.justzappit.offramp.p2p.RelayIdentity
import xyz.justzappit.offramp.p2p.RelayIdentityStore

/** EncryptedSharedPreferences-backed [RelayIdentityStore]. See its kdoc for why this must persist. */
class RelayIdentityStorageProvider(
    private val encryptedPreferenceProvider: EncryptedPreferenceProvider,
) : RelayIdentityStore {
    private val key = PreferenceKey(PREF_KEY)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun get(): RelayIdentity? = try {
        encryptedPreferenceProvider().getString(key)?.let { raw ->
            json.decodeFromString(RelayIdentity.serializer(), raw)
        }
    } catch (_: SerializationException) {
        null
    }

    override suspend fun set(identity: RelayIdentity) {
        encryptedPreferenceProvider().putString(
            key,
            json.encodeToString(RelayIdentity.serializer(), identity),
        )
    }

    companion object {
        private const val PREF_KEY = "p2p_offramp_relay_identity_v1"
    }
}

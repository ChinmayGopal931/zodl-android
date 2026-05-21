package xyz.justzappit.offramp.p2p

import xyz.justzappit.evm.hd.EvmKeyDerivation
import xyz.justzappit.evm.util.toHex
import java.security.SecureRandom

data class RelayIdentity(
    val privateKeyHex: String,
    val publicKeyHex: String,
)

object RelayIdentities {
    private const val FIELD_BYTES = 32
    private const val MAX_RETRIES = 100

    fun generate(): RelayIdentity {
        val random = SecureRandom()
        val buf = ByteArray(FIELD_BYTES)
        repeat(MAX_RETRIES) {
            random.nextBytes(buf)
            runCatching { EvmKeyDerivation.fromPrivateKey(buf) }
                .getOrNull()
                ?.let { key ->
                    return RelayIdentity(
                        privateKeyHex = "0x" + key.privateKey.toHex(),
                        publicKeyHex = key.publicKey.toHex(),
                    )
                }
        }
        error("Exhausted $MAX_RETRIES attempts to generate a relay identity")
    }
}

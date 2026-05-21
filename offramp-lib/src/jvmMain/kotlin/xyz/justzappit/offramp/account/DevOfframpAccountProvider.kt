package xyz.justzappit.offramp.account

import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.hd.EvmKeyDerivation
import xyz.justzappit.evm.util.hexToBytes

/**
 * Dev-only static EOA backed by a hardcoded private key. Used to ship the first end-to-end
 * UPI offramp flow on Base Sepolia before the rotating-burner [OfframpAccountProvider]
 * is wired up.
 *
 * **Testnet ONLY.** The private key is checked into source — anyone with this repo can
 * spend whatever sits in the account. Fund it with Sepolia ETH + USDC for local dev.
 * For mainnet use [StaticOfframpAccountProvider] backed by the user's actual wallet seed.
 */
object DevOfframpAccountProvider : OfframpAccountProvider {
    // Random 32 bytes, generated once for this repo. Not derived from any wallet seed.
    private const val DEV_PRIVATE_KEY_HEX =
        "0x46af9e1d2e3a8c5b71f9a0e6d4c2b8a31f5907e8d2c4a6f3e9b7d5c1a8f6e4d2"

    val key: EvmKey by lazy {
        EvmKeyDerivation.fromPrivateKey(DEV_PRIVATE_KEY_HEX.removePrefix("0x").hexToBytes())
    }

    val address: String get() = key.address

    override suspend fun nextOfframpAccount(): EvmKey = key
}

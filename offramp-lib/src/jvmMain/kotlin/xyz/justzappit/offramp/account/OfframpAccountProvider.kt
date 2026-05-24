package xyz.justzappit.offramp.account

import xyz.justzappit.evm.hd.EvmKey
import xyz.justzappit.evm.hd.EvmKeyDerivation

interface OfframpAccountProvider {
    suspend fun nextOfframpAccount(): EvmKey
}

class StaticOfframpAccountProvider(
    private val seedPhraseSource: SeedPhraseSource,
    private val fixedAccountIndex: Int = 0,
) : OfframpAccountProvider {
    override suspend fun nextOfframpAccount(): EvmKey =
        EvmKeyDerivation.derive(
            mnemonic = seedPhraseSource.getSeedPhrase(),
            accountIndex = fixedAccountIndex,
        )
}

package co.electriccoin.zcash.ui.common.provider

import xyz.justzappit.offramp.account.SeedPhraseSource

/**
 * Bridges the wallet's stored mnemonic to offramp-lib's [SeedPhraseSource] so the mainnet
 * [xyz.justzappit.offramp.account.StaticOfframpAccountProvider] can derive the ERC-4337 owner key
 * from the user's own seed (self-custodial), instead of the committed testnet dev key.
 */
class WalletSeedPhraseSource(
    private val persistableWalletProvider: PersistableWalletProvider,
) : SeedPhraseSource {
    override suspend fun getSeedPhrase(): String =
        persistableWalletProvider.requirePersistableWallet().seedPhrase.joinToString()
}

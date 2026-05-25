package co.electriccoin.zcash.ui.common.provider

import xyz.justzappit.offramp.account.SeedPhraseSource

/**
 * Bridges the wallet's stored mnemonic to offramp-lib's [SeedPhraseSource] so the mainnet
 * [xyz.justzappit.offramp.account.StaticOfframpAccountProvider] can derive the ERC-4337 owner key
 * from the user's own seed (self-custodial), instead of the committed testnet dev key.
 *
 * Returns the mnemonic as a fresh [CharArray] rather than an immutable `String`: callers can
 * zeroize after use, and we don't allocate a `SeedPhrase.joinToString()` intermediate that would
 * linger un-zeroizable in heap. The underlying word `String`s in the wallet's `List<String>` are
 * still immutable (not under our control), but at least we don't amplify the residue.
 */
class WalletSeedPhraseSource(
    private val persistableWalletProvider: PersistableWalletProvider,
) : SeedPhraseSource {
    override suspend fun getSeedPhrase(): CharArray {
        val words = persistableWalletProvider.requirePersistableWallet().seedPhrase.split
        if (words.isEmpty()) return CharArray(0)

        // total = sum of word lengths + (words.size - 1) single-space separators
        val totalLength = words.sumOf { it.length } + (words.size - 1)
        val out = CharArray(totalLength)
        var pos = 0
        for ((i, word) in words.withIndex()) {
            if (i > 0) {
                out[pos] = ' '
                pos++
            }
            word.toCharArray(out, pos, 0, word.length)
            pos += word.length
        }
        return out
    }
}

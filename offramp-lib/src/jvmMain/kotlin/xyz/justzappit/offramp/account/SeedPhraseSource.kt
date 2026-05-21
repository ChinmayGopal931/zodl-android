package xyz.justzappit.offramp.account

fun interface SeedPhraseSource {
    suspend fun getSeedPhrase(): String
}

package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.provider.PersistableWalletProvider
import co.electriccoin.zcash.ui.screen.chat.common.ChatError
import co.electriccoin.zcash.ui.screen.chat.common.ChatResult
import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class ExportChatSeedPhraseUseCase(
    private val sdk: ZappMessagingSDK,
    private val persistableWalletProvider: PersistableWalletProvider,
) {
    suspend operator fun invoke(): ChatResult<String> {
        // Chat identity is derived from the wallet seed (see ChatBootstrap.derive).
        // When a wallet exists, return its seed directly — this is the canonical
        // source and sidesteps the round-trip through the SDK. Falls back to the
        // SDK only for identities created via createIdentity() with no wallet.
        val wallet = persistableWalletProvider.getPersistableWallet()
        if (wallet != null) {
            return ChatResult.Success(wallet.seedPhrase.joinedString())
        }

        return runChatCallResult("ExportChatSeedPhraseUseCase: exportSeedPhrase failed") {
            sdk.exportSeedPhrase()
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(ChatError.ExportSeedPhraseFailed) },
        )
    }
}

private fun cash.z.ecc.android.sdk.model.SeedPhrase.joinedString(): String =
    split.joinToString(" ")

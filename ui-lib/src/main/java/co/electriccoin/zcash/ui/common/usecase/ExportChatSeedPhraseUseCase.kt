package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.common.ChatError
import co.electriccoin.zcash.ui.screen.chat.common.ChatResult
import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class ExportChatSeedPhraseUseCase(
    private val sdk: ZappMessagingSDK,
    private val chatBootstrap: ChatBootstrap,
) {
    suspend operator fun invoke(): ChatResult<String> {
        // Prefer the seed we explicitly passed to restoreFromSeedPhrase during
        // auto-derivation. This sidesteps a potential SDK-side shadow-cache
        // mismatch where exportSeedPhrase() may not reflect the seed used in
        // restoreFromSeedPhrase(). Falls back to the SDK once the JS-layer fix
        // is confirmed (or for identities created via createIdentity()).
        val local = chatBootstrap.derivedSeedPhrase.value
        if (local != null) return ChatResult.Success(local)

        return runChatCallResult("ExportChatSeedPhraseUseCase: exportSeedPhrase failed") {
            sdk.exportSeedPhrase()
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(ChatError.ExportSeedPhraseFailed) },
        )
    }
}

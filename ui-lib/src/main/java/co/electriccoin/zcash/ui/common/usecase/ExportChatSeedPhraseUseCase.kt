package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.ChatError
import co.electriccoin.zcash.ui.screen.chat.common.ChatResult
import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class ExportChatSeedPhraseUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke(): ChatResult<String> =
        runChatCallResult("ExportChatSeedPhraseUseCase: exportSeedPhrase failed") {
            sdk.exportSeedPhrase()
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(ChatError.ExportSeedPhraseFailed) },
        )
}

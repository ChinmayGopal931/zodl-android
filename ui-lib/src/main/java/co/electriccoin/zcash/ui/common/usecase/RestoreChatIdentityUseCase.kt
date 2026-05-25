package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.ChatError
import co.electriccoin.zcash.ui.screen.chat.common.ChatResult
import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class RestoreChatIdentityUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke(seedPhrase: String, displayName: String): ChatResult<Unit> =
        runChatCallResult("RestoreChatIdentityUseCase: restoreFromSeedPhrase failed") {
            sdk.restoreFromSeedPhrase(seedPhrase, displayName)
        }.fold(
            onSuccess = { ChatResult.Success(Unit) },
            onFailure = { ChatResult.Failure(ChatError.RestoreIdentityFailed) },
        )
}

package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.ChatError
import co.electriccoin.zcash.ui.screen.chat.common.ChatResult
import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class CreateChatIdentityUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke(displayName: String): ChatResult<String> =
        runChatCallResult("CreateChatIdentityUseCase: createIdentity failed") {
            sdk.createIdentity(displayName).seedPhrase
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(ChatError.CreateIdentityFailed) },
        )
}

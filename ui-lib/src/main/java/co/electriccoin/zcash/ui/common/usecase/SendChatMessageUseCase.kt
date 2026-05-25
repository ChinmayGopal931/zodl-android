package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import xyz.justzappit.zappmessaging.models.ZMMessage

class SendChatMessageUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke(
        conversationId: String,
        content: String,
        contentType: String? = null,
    ): Result<ZMMessage> =
        runChatCallResult("SendChatMessageUseCase: sendMessage failed") {
            if (contentType == null) {
                sdk.sendMessage(conversationId = conversationId, content = content)
            } else {
                sdk.sendMessage(conversationId, content, contentType)
            }
        }
}

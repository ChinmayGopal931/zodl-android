package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class DeleteChatIdentityUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke() {
        runChatCall("DeleteChatIdentityUseCase: shutdown failed") {
            sdk.shutdown()
        }
    }
}

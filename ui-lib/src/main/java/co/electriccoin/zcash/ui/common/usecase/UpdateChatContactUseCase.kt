package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class UpdateChatContactUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke(publicKey: String, newName: String): Result<Unit> =
        runChatCallResult("UpdateChatContactUseCase: updateContact failed") {
            sdk.updateContact(publicKey, newName)
        }
}

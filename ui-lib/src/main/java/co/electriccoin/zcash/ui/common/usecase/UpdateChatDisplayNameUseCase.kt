package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import xyz.justzappit.zappmessaging.ZappMessagingSDK

class UpdateChatDisplayNameUseCase(
    private val sdk: ZappMessagingSDK,
) {
    suspend operator fun invoke(displayName: String) {
        runChatCall("UpdateChatDisplayNameUseCase: updateDisplayName failed") {
            sdk.updateDisplayName(displayName)
        }
    }
}

package co.electriccoin.zcash.ui.common.usecase

import xyz.justzappit.zappmessaging.ZappMessagingSDK

class ObserveChatOnlineStateUseCase(
    private val sdk: ZappMessagingSDK,
) {
    operator fun invoke() = sdk.isOnline
}

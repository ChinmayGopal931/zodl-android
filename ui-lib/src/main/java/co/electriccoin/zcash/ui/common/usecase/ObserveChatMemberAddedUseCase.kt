package co.electriccoin.zcash.ui.common.usecase

import xyz.justzappit.zappmessaging.ZappMessagingSDK

class ObserveChatMemberAddedUseCase(
    private val sdk: ZappMessagingSDK,
) {
    operator fun invoke() = sdk.memberAdded
}

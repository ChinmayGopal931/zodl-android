package co.electriccoin.zcash.ui.screen.chat.support

sealed interface SupportChatEffect {
    data object PickMedia : SupportChatEffect

    data object PickFile : SupportChatEffect

    data object TakePhoto : SupportChatEffect
}

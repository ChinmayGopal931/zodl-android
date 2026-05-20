package co.electriccoin.zcash.ui.screen.chat.contacts

import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.screen.chat.model.ChatContact

data class ChatContactsState(
    val title: StringResource,
    val contacts: List<ChatContact>,
    val showBackButton: Boolean,
    val onStartChat: (publicKey: String) -> Unit,
    val onAddSheetOpen: () -> Unit,
    val onEditSheetOpen: (ChatContact) -> Unit,
    val onBack: () -> Unit,
    val addSheet: AddChatContactState?,
    val editSheet: EditChatContactState?,
)

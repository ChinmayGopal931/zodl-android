package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.screen.chat.ChatRoomArgs
import co.electriccoin.zcash.ui.screen.chat.contactedit.ContactEditVM
import co.electriccoin.zcash.ui.screen.chat.contacts.ChatContactsVM
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupVM
import co.electriccoin.zcash.ui.screen.chat.list.ChatListVM
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationVM
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileVM
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomVM
import co.electriccoin.zcash.ui.screen.chat.scan.ChatScanPublicKeyVM
import co.electriccoin.zcash.ui.screen.chat.settings.ChatSettingsVM
import co.electriccoin.zcash.ui.screen.chat.support.SupportChatVM
import co.electriccoin.zcash.ui.screen.chat.support.SupportTicketListVM
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatViewModelModule =
    module {
        viewModelOf(::ChatListVM)
        viewModelOf(::ChatIdentitySetupVM)
        viewModelOf(::ChatProfileVM)
        viewModelOf(::ChatSettingsVM)
        viewModelOf(::ChatContactsVM)
        viewModelOf(::NewConversationVM)
        viewModelOf(::ContactEditVM)
        // ChatRoomVM is constructed manually because it takes a runtime [ChatRoomArgs] parameter.
        viewModel { (args: ChatRoomArgs) ->
            ChatRoomVM(
                args = args,
                application = get(),
                moderationRepository = get(),
                chatConversationsRepository = get(),
                getZashiAccount = get(),
                chatSendContext = get(),
                navigationRouter = get(),
                observeChatMessageReceived = get(),
                observeChatMessageStatus = get(),
                observeChatMediaDownloadComplete = get(),
                observeChatPeerStatus = get(),
                getChatMessages = get(),
                sendChatMessage = get(),
                sendChatMediaMessage = get(),
                getChatConnectionDetails = get(),
                updateChatContact = get(),
            )
        }
        viewModelOf(::ChatScanPublicKeyVM)
        viewModelOf(::SupportTicketListVM)
        viewModelOf(::SupportChatVM)
    }

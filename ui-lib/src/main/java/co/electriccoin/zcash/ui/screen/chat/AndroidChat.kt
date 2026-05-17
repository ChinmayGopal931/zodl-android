package co.electriccoin.zcash.ui.screen.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.usecase.ObserveSelectedWalletAccountUseCase
import co.electriccoin.zcash.ui.screen.chat.view.ChatContactsView
import co.electriccoin.zcash.ui.screen.chat.view.ChatIdentitySetupView
import co.electriccoin.zcash.ui.screen.chat.view.ChatListView
import co.electriccoin.zcash.ui.screen.chat.view.ChatProfileView
import co.electriccoin.zcash.ui.screen.chat.view.ChatRoomView
import co.electriccoin.zcash.ui.screen.chat.view.ChatSettingsView
import co.electriccoin.zcash.ui.screen.chat.view.ContactEditView
import co.electriccoin.zcash.ui.screen.chat.view.NewConversationView
import co.electriccoin.zcash.ui.screen.chat.viewmodel.ChatViewModel
import co.electriccoin.zcash.ui.common.usecase.ChatSendContext
import co.electriccoin.zcash.ui.screen.unifiedsend.UnifiedSendArgs
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ── Navigation route args ───────────────────────────────────────────────

@Serializable
object ChatHomeArgs

@Serializable
data class ChatRoomArgs(val conversationId: String)

@Serializable
object NewConversationArgs

@Serializable
object ChatContactsArgs

@Serializable
object ChatProfileArgs

@Serializable
object ChatSettingsArgs

@Serializable
data class ContactEditArgs(val publicKey: String)

// ── Entry point composables ─────────────────────────────────────────────

@Composable
fun AndroidChatHome(
    onNavigateToChatRoom: (String) -> Unit,
    onNavigateToNewConversation: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val identity by viewModel.identity.collectAsState()

    when {
        isInitializing -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        identity == null -> {
            ChatIdentitySetupView(
                onSetupComplete = { /* Identity set, recomposition will show chat list */ },
                viewModel = viewModel
            )
        }
        else -> {
            ChatListView(
                onConversationClick = { conversation ->
                    viewModel.setCurrentConversation(conversation)
                    onNavigateToChatRoom(conversation.id)
                },
                onNewMessage = onNavigateToNewConversation,
                onNavigateBack = onNavigateBack,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun AndroidChatRoom(
    conversationId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()
    val navigationRouter = koinInject<NavigationRouter>()
    val chatSendContext = koinInject<ChatSendContext>()

    ChatRoomView(
        conversationId = conversationId,
        onNavigateBack = onNavigateBack,
        onSendZec = {
            val peerWalletAddress = viewModel.getPeerWalletAddress(conversationId)
            chatSendContext.set(conversationId)
            navigationRouter.forward(UnifiedSendArgs(recipientAddress = peerWalletAddress))
        },
        viewModel = viewModel
    )
}

@Composable
fun AndroidNewConversation(
    onConversationCreated: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()

    NewConversationView(
        onConversationCreated = onConversationCreated,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

@Composable
fun AndroidChatContacts(
    onStartChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: ChatViewModel = koinViewModel()

    ChatContactsView(
        onStartChat = { publicKey ->
            viewModel.createDirectChat(publicKey) { conversationId ->
                onStartChat(conversationId)
            }
        },
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

@Composable
fun AndroidChatProfile(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onIdentityDeleted: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()
    val observeWalletAccount: ObserveSelectedWalletAccountUseCase = koinInject()
    val walletAccount by observeWalletAccount().collectAsState(initial = null)

    ChatProfileView(
        onNavigateBack = onNavigateBack,
        onNavigateToContacts = onNavigateToContacts,
        onIdentityDeleted = onIdentityDeleted,
        walletAccount = walletAccount,
        viewModel = viewModel,
    )
}

@Composable
fun AndroidChatSettings(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onIdentityDeleted: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()

    ChatSettingsView(
        onNavigateBack = onNavigateBack,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToContacts = onNavigateToContacts,
        onIdentityDeleted = onIdentityDeleted,
        viewModel = viewModel
    )
}

@Composable
fun AndroidContactEdit(
    publicKey: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChatViewModel = koinViewModel()

    ContactEditView(
        publicKey = publicKey,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

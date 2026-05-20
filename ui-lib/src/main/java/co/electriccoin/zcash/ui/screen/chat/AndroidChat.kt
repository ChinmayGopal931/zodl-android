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
import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.contactedit.ContactEditScreen
import co.electriccoin.zcash.ui.screen.chat.contacts.ChatContactsScreen
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupScreen
import co.electriccoin.zcash.ui.screen.chat.identity.ChatIdentitySetupVM
import co.electriccoin.zcash.ui.screen.chat.list.ChatListScreen
import co.electriccoin.zcash.ui.screen.chat.newconv.NewConversationScreen
import co.electriccoin.zcash.ui.screen.chat.profile.ChatProfileScreen
import co.electriccoin.zcash.ui.screen.chat.room.ChatRoomScreen
import co.electriccoin.zcash.ui.screen.chat.settings.ChatSettingsScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ── Navigation route args ───────────────────────────────────────────────

@Serializable
object ChatHomeArgs

@Serializable
data class ChatRoomArgs(
    val conversationId: String
)

@Serializable
object NewConversationArgs

@Serializable
object ChatContactsArgs

@Serializable
object ChatProfileArgs

@Serializable
object ChatSettingsArgs

@Serializable
data class ContactEditArgs(
    val publicKey: String
)

// ── Entry point composables ─────────────────────────────────────────────

@Composable
fun AndroidChatHome() {
    val bootstrap: ChatBootstrap = koinInject()
    val identitySetupVm: ChatIdentitySetupVM = koinViewModel()
    val isInitializing by bootstrap.isInitializing.collectAsState()
    val isSetupComplete by identitySetupVm.isSetupComplete.collectAsState()

    when {
        isInitializing -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        !isSetupComplete -> {
            ChatIdentitySetupScreen()
        }

        else -> {
            ChatListScreen(
                onLegacyConversationSelected = { conv -> bootstrap.markConversationRead(conv.id) }
            )
        }
    }
}

@Composable
fun AndroidChatRoom(conversationId: String) {
    ChatRoomScreen(conversationId = conversationId)
}

@Composable
fun AndroidNewConversation() {
    NewConversationScreen()
}

@Composable
fun AndroidChatContacts() {
    ChatContactsScreen()
}

@Composable
fun AndroidChatProfile() {
    ChatProfileScreen()
}

@Composable
fun AndroidChatSettings() {
    ChatSettingsScreen()
}

@Composable
fun AndroidContactEdit(publicKey: String) {
    ContactEditScreen(publicKey = publicKey)
}

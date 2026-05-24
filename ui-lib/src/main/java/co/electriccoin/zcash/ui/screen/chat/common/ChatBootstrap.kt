package co.electriccoin.zcash.ui.screen.chat.common

import android.app.Application
import co.electriccoin.zcash.ui.common.provider.PersistableWalletProvider
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import xyz.justzappit.zappmessaging.models.ZMIdentity

class ChatBootstrap(
    private val application: Application,
    private val sdk: ZappMessagingSDK,
    private val moderationRepository: ChatModerationRepository,
    private val persistableWalletProvider: PersistableWalletProvider,
) {
    // Bare-kit's native IPC init has main-thread affinity; running off-main
    // null-derefs in `bare_ipc_poll_init`.
    private val scope = MainScope()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val totalUnreadCount: StateFlow<Int> =
        unreadCounts
            .map { it.values.sum() }
            .stateIn(scope, SharingStarted.Eagerly, 0)

    val identity: StateFlow<ZMIdentity?> get() = sdk.identity

    init {
        scope.launch {
            try {
                runChatCall("ChatBootstrap: sdk.initialize failed") {
                    sdk.initialize(application)
                }
            } finally {
                _isInitializing.value = false
            }
        }
        scope.launch { observeMessagesForUnread() }
    }

    private suspend fun observeMessagesForUnread() {
        sdk.messageReceived.collect { (conversationId, msg) ->
            if (msg.isFromMe) return@collect
            if (moderationRepository.isBlocked(msg.senderId)) return@collect
            unreadCounts.update { current ->
                current + (conversationId to ((current[conversationId] ?: 0) + 1))
            }
        }
    }

    fun markConversationRead(conversationId: String) {
        unreadCounts.update { it - conversationId }
    }

    suspend fun restoreFromWalletSeed(displayName: String): ZMIdentity {
        val wallet =
            persistableWalletProvider.persistableWallet.first { it != null }
                ?: error("Wallet unavailable")
        val seedWords = wallet.seedPhrase.split.joinToString(" ")
        return sdk.restoreFromSeedPhrase(seedWords, displayName)
    }

    suspend fun restoreFromSeedPhrase(seedPhrase: String, displayName: String): ZMIdentity =
        sdk.restoreFromSeedPhrase(seedPhrase, displayName)
}

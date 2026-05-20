package co.electriccoin.zcash.ui.screen.chat.common

import android.app.Application
import co.electriccoin.zcash.spackle.Twig
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

/**
 * Application-scoped bootstrap for the chat subsystem. Owns the one-shot
 * `sdk.initialize` call, the aggregate unread-count counter that the tabs
 * shell renders on the chat tab badge, and the `restoreFromWalletSeed`
 * entry point used during onboarding.
 *
 * Per-screen state lives on the per-screen `*VM` classes; cross-screen
 * state lives here. Registered as a Koin `single` and constructed lazily
 * on first request — by the time any chat screen is rendered, init has
 * been kicked off.
 */
@Suppress("TooGenericExceptionCaught")
class ChatBootstrap(
    private val application: Application,
    private val sdk: ZappMessagingSDK,
    private val moderationRepository: ChatModerationRepository,
    private val persistableWalletProvider: PersistableWalletProvider,
) {
    // Bare-kit's native IPC init (called from `sdk.initialize`) has main-thread
    // affinity — the legacy ChatViewModel ran initialize() inside viewModelScope
    // (Dispatchers.Main.immediate). Mirror that here, otherwise bare_ipc_poll_init
    // null-derefs on a worker thread.
    private val scope = MainScope()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val totalUnreadCount: StateFlow<Int> =
        _unreadCounts
            .map { it.values.sum() }
            .stateIn(scope, SharingStarted.Eagerly, 0)

    val identity: StateFlow<ZMIdentity?> get() = sdk.identity

    init {
        scope.launch {
            try {
                sdk.initialize(application)
            } catch (e: Exception) {
                Twig.warn(e) { "ChatBootstrap: sdk.initialize failed" }
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
            _unreadCounts.update { current ->
                current + (conversationId to ((current[conversationId] ?: 0) + 1))
            }
        }
    }

    fun markConversationRead(conversationId: String) {
        _unreadCounts.update { it - conversationId }
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

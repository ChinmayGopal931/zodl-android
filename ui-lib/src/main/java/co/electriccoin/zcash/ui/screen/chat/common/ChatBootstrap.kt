package co.electriccoin.zcash.ui.screen.chat.common

import android.app.Application
import cash.z.ecc.android.sdk.model.PersistableWallet
import co.electriccoin.zcash.ui.common.provider.PersistableWalletProvider
import co.electriccoin.zcash.ui.common.provider.joinedSeedPhraseChars
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val pendingDisplayName = MutableStateFlow<String?>(null)

    private val _chatIdentityError = MutableStateFlow<Throwable?>(null)
    val chatIdentityError: StateFlow<Throwable?> = _chatIdentityError.asStateFlow()

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
        scope.launch { observePendingChatIdentityDerivation() }
    }

    /**
     * Records the display name the user picked during onboarding. The reactive auto-derive
     * coroutine will pick it up once the SDK is initialised and a wallet seed becomes
     * available, and derive the chat identity from that seed. Safe to call before either
     * is ready — the request is queued. Idempotent.
     */
    fun setPendingDisplayName(displayName: String) {
        _chatIdentityError.value = null
        pendingDisplayName.value = displayName
    }

    fun clearChatIdentityError() {
        _chatIdentityError.value = null
    }

    private suspend fun observePendingChatIdentityDerivation() {
        combine(
            _isInitializing,
            persistableWalletProvider.persistableWallet,
            sdk.identity,
            pendingDisplayName,
        ) { initializing, wallet, identity, name ->
            buildAutoDeriveRequest(initializing, wallet, identity, name)
        }
            .distinctUntilChanged()
            .collect { request ->
                if (request != null) derive(request)
            }
    }

    private fun buildAutoDeriveRequest(
        initializing: Boolean,
        wallet: PersistableWallet?,
        identity: ZMIdentity?,
        name: String?,
    ): AutoDeriveRequest? {
        if (initializing || wallet == null || identity != null || name.isNullOrBlank()) return null
        return AutoDeriveRequest(wallet = wallet, displayName = name)
    }

    private suspend fun derive(request: AutoDeriveRequest) {
        val seedChars = request.wallet.joinedSeedPhraseChars()
        try {
            runChatCallResult("ChatBootstrap: auto-derive chat identity from wallet seed") {
                sdk.restoreFromSeedPhrase(seedChars, request.displayName)
            }.fold(
                onSuccess = {
                    pendingDisplayName.value = null
                    _chatIdentityError.value = null
                },
                onFailure = { e ->
                    _chatIdentityError.value = e
                },
            )
        } finally {
            seedChars.fill(' ')
        }
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

    suspend fun restoreFromSeedPhrase(seedPhrase: String, displayName: String): ZMIdentity =
        sdk.restoreFromSeedPhrase(seedPhrase, displayName)

    private data class AutoDeriveRequest(val wallet: PersistableWallet, val displayName: String)
}

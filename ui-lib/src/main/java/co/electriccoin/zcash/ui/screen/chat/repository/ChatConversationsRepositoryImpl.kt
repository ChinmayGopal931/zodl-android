package co.electriccoin.zcash.ui.screen.chat.repository

import co.electriccoin.zcash.preference.StandardPreferenceProvider
import co.electriccoin.zcash.ui.common.provider.ChatNotifier
import co.electriccoin.zcash.ui.preference.StandardPreferenceKeys
import co.electriccoin.zcash.ui.screen.chat.common.runChatCall
import co.electriccoin.zcash.ui.screen.chat.common.runChatCallResult
import co.electriccoin.zcash.ui.screen.chat.model.ChatConversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.zappmessaging.ZappMessagingSDK
import xyz.justzappit.zappmessaging.models.ZMMessage

class ChatConversationsRepositoryImpl(
    private val sdk: ZappMessagingSDK,
    private val moderationRepository: ChatModerationRepository,
    private val chatNotifier: ChatNotifier,
    standardPreferenceProvider: StandardPreferenceProvider,
) : ChatConversationsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _conversations = MutableStateFlow<List<ChatConversation>?>(null)
    override val conversations: StateFlow<List<ChatConversation>?> = _conversations.asStateFlow()

    override val localPublicKey: StateFlow<String?> =
        sdk.identity
            .map { it?.publicKey }
            .stateIn(scope, SharingStarted.Eagerly, null)

    override val isOnline: StateFlow<Boolean> get() = sdk.isOnline
    override val peerCount: StateFlow<Int> get() = sdk.peerCount
    override val dhtHealth: StateFlow<String> get() = sdk.dhtHealth
    override val conversationDeleted: SharedFlow<String> get() = sdk.groupDeleted

    private val activeConversationId = MutableStateFlow<String?>(null)
    private var refreshJob: Job? = null

    // Nullable until the persisted value loads; maybeNotify treats "not yet known" as
    // "don't notify" so a message in the cold-start window can't fire against a setting
    // the user previously turned off.
    private val notificationsEnabled: StateFlow<Boolean?> =
        flow {
            emitAll(StandardPreferenceKeys.IS_CHAT_NOTIFICATIONS_ENABLED.observe(standardPreferenceProvider()))
        }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch { observeIdentityAndRefresh() }
        observeConversationEvents()
    }

    override suspend fun refresh() {
        runChatCallResult("ChatConversationsRepository: failed to refresh conversations") {
            sdk.refreshConversations()
            sdk.conversations.value
                .map(ChatConversation::from)
                .sortedByDescending { it.lastMessageTimestamp ?: 0L }
        }.onSuccess { list ->
            _conversations.value = list
        }.onFailure {
            // Surface an empty list so subscribers leave the loading state.
            if (_conversations.value == null) _conversations.value = emptyList()
        }
    }

    override fun markConversationRead(conversationId: String) {
        _conversations.update { current ->
            current?.map { c -> if (c.id == conversationId) c.copy(unreadCount = 0) else c }
        }
    }

    override fun setActiveConversation(conversationId: String?) {
        activeConversationId.value = conversationId
    }

    override fun renameConversation(conversationId: String, newName: String) {
        _conversations.update { list ->
            list?.map { conv -> if (conv.id == conversationId) conv.copy(displayName = newName) else conv }
        }
    }

    override suspend fun leaveConversation(conversationId: String) {
        runChatCall("ChatConversationsRepository: leave conversation failed") {
            sdk.removeConversation(conversationId)
            _conversations.update { it?.filter { c -> c.id != conversationId } }
        }
    }

    override fun conversation(conversationId: String): Flow<ChatConversation?> =
        conversations.map { list -> list?.firstOrNull { it.id == conversationId } }

    private suspend fun observeIdentityAndRefresh() {
        sdk.identity.collect { id ->
            if (id != null) startConversationRefresh()
        }
    }

    private fun startConversationRefresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch { refresh() }
    }

    private fun observeConversationEvents() {
        scope.launch {
            sdk.messageReceived.collect { (conversationId, msg) ->
                if (moderationRepository.isBlocked(msg.senderId)) return@collect
                val isViewingConversation = conversationId == activeConversationId.value
                maybeNotify(conversationId, msg, isViewingConversation)
                _conversations.update { current ->
                    current?.map { conv ->
                        if (conv.id == conversationId) {
                            conv.copy(
                                lastMessage =
                                    msg.content.ifEmpty {
                                        ChatConversationsRepository.MEDIA_PLACEHOLDER_SENTINEL
                                    },
                                lastMessageTimestamp = msg.timestamp,
                                unreadCount =
                                    when {
                                        msg.isFromMe -> conv.unreadCount
                                        isViewingConversation -> conv.unreadCount
                                        else -> conv.unreadCount + 1
                                    },
                            )
                        } else {
                            conv
                        }
                    }
                }
            }
        }
        scope.launch {
            sdk.inviteReceived.collect {
                delay(CONVERSATION_RELOAD_DEBOUNCE_MS)
                refresh()
            }
        }
        scope.launch {
            sdk.groupDeleted.collect { conversationId ->
                _conversations.update { it?.filter { conv -> conv.id != conversationId } }
            }
        }
        scope.launch {
            sdk.groupRenamed.collect { (conversationId, newName) ->
                renameConversation(conversationId, newName)
            }
        }
        scope.launch {
            sdk.memberLeft.collect { (conversationId, peerKey) ->
                _conversations.update { list ->
                    list?.map { conv ->
                        if (conv.id == conversationId) {
                            conv.copy(participantIds = conv.participantIds.filter { it != peerKey })
                        } else {
                            conv
                        }
                    }
                }
            }
        }
        scope.launch {
            sdk.memberAdded.collect { (conversationId, peerKey, _) ->
                _conversations.update { list ->
                    list?.map { conv ->
                        if (conv.id == conversationId && peerKey !in conv.participantIds) {
                            conv.copy(participantIds = conv.participantIds + peerKey)
                        } else {
                            conv
                        }
                    }
                }
            }
        }
    }

    private fun maybeNotify(
        conversationId: String,
        msg: ZMMessage,
        isViewingConversation: Boolean,
    ) {
        if (msg.isFromMe || isViewingConversation || notificationsEnabled.value != true) return
        val conversationName = _conversations.value?.firstOrNull { it.id == conversationId }?.displayName
        chatNotifier.post(
            conversationId = conversationId,
            conversationName = conversationName,
            senderName = msg.senderName,
            content = msg.content,
        )
    }

    companion object {
        private const val CONVERSATION_RELOAD_DEBOUNCE_MS = 500L
    }
}
